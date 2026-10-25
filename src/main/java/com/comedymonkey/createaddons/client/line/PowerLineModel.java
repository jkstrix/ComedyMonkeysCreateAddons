package com.comedymonkey.createaddons.client.line;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.line.Fitting;
import com.comedymonkey.createaddons.content.line.LineTier;
import com.comedymonkey.createaddons.content.line.PowerLineBlock;
import com.comedymonkey.createaddons.content.line.PowerLineBlockEntity;
import com.comedymonkey.createaddons.content.line.WireLink;
import com.mojang.math.Transformation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * A Power Line block's model, built from its block entity: for every face a wire lies on, the middle piece, an arm to
 * each edge it runs off (short of a wire up the next face, with a fold over an outside edge, and up a machine's face to a
 * socket in its middle), a riser straight up through the block into a machine across it,
 * a clip on a straight run and a splice at a junction. Where it's threaded through a drilled wall, a surround round the
 * hole on the cable's free sides. The parts are authored once, lying on the floor and running
 * north, and turned to each face and direction. Bare copper and brass twin flex, or the thicker covered cable a dyed wire
 * becomes; the body is tinted by tier or dye.
 */
public class PowerLineModel extends EncasedLineModel {
	private static final ModelProperty<String> WIRES = new ModelProperty<>();
	private static final String[] STYLES = {"copper", "brass", "copper_covered", "brass_covered"};
	private static final String[] PARTS = {"centre", "arm", "arm_inner", "fold", "clip", "splice", "rise", "socket", "riser", "grommet", "hole_edge", "hole",
		"hole_bar_long", "hole_bar_short"};
	private static final Map<String, List<BakedQuad>> CACHE = new ConcurrentHashMap<>();

	public PowerLineModel(BakedModel original) {
		super(original);
	}

	public static void registerModels(ModelEvent.RegisterAdditional event) {
		for (String style : STYLES)
			for (String part : PARTS)
				event.register(part(style, part));
	}

	/** The parts are read again after a resource reload. */
	public static void forget() {
		CACHE.clear();
	}

	private static ModelResourceLocation part(String style, String part) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/power_line/" + style + "_" + part));
	}

	@Override
	public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
		data = super.getModelData(level, pos, state, data);
		if (!(level.getBlockEntity(pos) instanceof PowerLineBlockEntity be))
			return data;
		StringBuilder key = new StringBuilder(style(be.tier(), be.dye() != null));
		key.append(be.fitting() != Fitting.NONE ? ";f" : ";-");
		for (Direction face : Direction.values())
			key.append(';').append(PowerLineBlock.hasFace(state, face) ? Integer.toString(be.links(face)) : "x");
		return data.derive().with(WIRES, key.toString()).build();
	}

	private static String style(LineTier tier, boolean covered) {
		return (tier == LineTier.BRASS ? "brass" : "copper") + (covered ? "_covered" : "");
	}

	@Override
	protected List<BakedQuad> lineQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data,
										@Nullable RenderType renderType) {
		String key = data.get(WIRES);
		if (key == null || side != null || (renderType != null && renderType != RenderType.cutout()))
			return List.of();
		return CACHE.computeIfAbsent(key, PowerLineModel::build);
	}

	private static List<BakedQuad> build(String key) {
		String[] parts = key.split(";");
		String style = parts[0];
		boolean fitted = parts[1].equals("f");
		List<BakedQuad> out = new ArrayList<>();
		for (Direction face : Direction.values()) {
			String packed = parts[2 + face.ordinal()];
			if (packed.equals("x"))
				continue;
			int links = Integer.parseInt(packed);
			List<Direction> across = WireLink.across(face);
			add(out, style, "centre", face, across.get(0));
			int count = 0;
			Direction run = null;
			boolean straight = false;
			// a hole into the wall it lies on: at an edge (the wire runs to it there) or in the middle
			Direction holeEdge = WireLink.holeEdge(links, face);
			boolean holeMiddle = WireLink.hole(links) == 1;
			for (Direction d : across) {
				WireLink link = WireLink.get(links, face, d);
				if (d == holeEdge) {
					add(out, style, "hole_edge", face, d);
					if (!link.connected())
						link = WireLink.LINE;
				}
				if (!link.connected())
					continue;
				count++;
				if (run == null)
					run = d;
				else if (d == run.getOpposite())
					straight = true;
				// into a machine: up its face to the middle and into a socket there
				add(out, style, link == WireLink.INSIDE || link == WireLink.TERMINAL ? "arm_inner" : "arm", face, d);
				if (link == WireLink.OUTSIDE)
					add(out, style, "fold", face, d);
				if (link == WireLink.TERMINAL) {
					add(out, style, "rise", face, d);
					add(out, style, "socket", face, d);
				}
				if (link == WireLink.THROUGH)
					add(out, style, "grommet", face, d);
			}
			// straight up through the block into a machine across it, a socket in the middle of its face
			boolean riser = (links & WireLink.RISE) != 0;
			if (riser)
				add(out, style, "riser", face, across.get(0));
			if (holeMiddle) {
				add(out, style, "hole", face, across.get(0));
				for (Direction d : across)
					if (!WireLink.get(links, face, d).connected())
						add(out, style, d.getAxis() == across.get(0).getAxis() ? "hole_bar_long" : "hole_bar_short", face, d);
				// the surround sits where the clip would
				continue;
			}
			if (fitted)
				continue;
			if (count >= 3)
				add(out, style, "splice", face, across.get(0));
			else if (count <= 1 || straight)
				add(out, style, "clip", face, run == null ? across.get(0) : run);
		}
		return out;
	}

	/** A part's quads, turned from lying on the floor running north to lying on {@code face} running {@code d}. */
	private static void add(List<BakedQuad> out, String style, String part, Direction face, Direction d) {
		BakedModel model = Minecraft.getInstance().getModelManager().getModel(part(style, part));
		List<BakedQuad> quads = new ArrayList<>();
		RandomSource rand = RandomSource.create(42);
		for (Direction s : Direction.values())
			quads.addAll(model.getQuads(null, s, rand, ModelData.EMPTY, null));
		quads.addAll(model.getQuads(null, null, rand, ModelData.EMPTY, null));
		Matrix3f m = rotation(face, d);
		// the turned quads face their new way, so they're shaded for it
		for (BakedQuad q : QuadTransformers.applying(turn(m)).process(quads)) {
			Vector3f n = m.transform(new Vector3f(q.getDirection().getStepX(), q.getDirection().getStepY(), q.getDirection().getStepZ()));
			Direction facing = Direction.getNearest(n.x, n.y, n.z);
			out.add(new BakedQuad(q.getVertices(), q.getTintIndex(), facing, q.getSprite(), q.isShade(), q.hasAmbientOcclusion()));
		}
	}

	/**
	 * The turn taking the floor (down) to {@code face} and north to {@code d}, about the block's middle: x goes to
	 * face × d, y to the face's opposite, z to d's opposite.
	 */
	static Matrix3f rotation(Direction face, Direction d) {
		Vector3f f = new Vector3f(face.getStepX(), face.getStepY(), face.getStepZ());
		Vector3f r = new Vector3f(d.getStepX(), d.getStepY(), d.getStepZ());
		Vector3f x = new Vector3f(f).cross(r);
		return new Matrix3f(x.x, x.y, x.z, -f.x, -f.y, -f.z, -r.x, -r.y, -r.z);
	}

	static Transformation turn(Matrix3f m) {
		Matrix4f t = new Matrix4f().translate(0.5f, 0.5f, 0.5f).mul(new Matrix4f(m)).translate(-0.5f, -0.5f, -0.5f);
		return new Transformation(t);
	}
}
