package com.comedymonkey.createaddons.client.line;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.LightRenderer;
import com.comedymonkey.createaddons.content.light.LightBlock;
import com.comedymonkey.createaddons.content.light.LightStripBlock;
import com.comedymonkey.createaddons.content.light.StripLinks;
import com.comedymonkey.createaddons.content.line.WireLink;
import com.comedymonkey.createaddons.registry.ModBlocks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * A Light Strip block's model, built from how its strips join (StripLinks), from parts authored lying on the floor and
 * running north (design/models/electric-lights.export.js), turned to each face and direction as Power Line wires are.
 * A strip that runs one way (or not yet anywhere) goes straight through the block to its edges, capped where nothing
 * meets it; at a junction or a turn it's the middle and an arm to each strip it meets. Up the next face of the same block
 * the arm stops short and one of the two draws the corner; over an edge both draw the fold. The diffuser is lit glass,
 * full-bright and tinted by the block's dye and brightness (its block colour), while the strip is lit, else the unlit
 * glass. All of it is baked into the world: a strip has no renderer, and only changes when the strips or its state do.
 */
public class StripModel extends BakedModelWrapper<BakedModel> {
	private static final ModelProperty<String> STRIPS = new ModelProperty<>();
	private static final String[] PARTS = {"centre", "arm", "arm_inner", "corner", "fold", "cap"};
	/** The parts with a diffuser over the channel. */
	private static final List<String> LIT = List.of("arm", "arm_inner", "fold");
	private static final Map<String, List<BakedQuad>> CACHE = new ConcurrentHashMap<>();

	public StripModel(BakedModel original) {
		super(original);
	}

	private static ModelResourceLocation part(String name) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/light_strip/" + name));
	}

	public static void registerModels(ModelEvent.RegisterAdditional event) {
		for (String p : PARTS) {
			event.register(part(p));
			if (LIT.contains(p)) {
				event.register(part(p + "_glow"));
				event.register(part(p + "_glass"));
			}
		}
	}

	/** The parts are read again after a resource reload. */
	public static void forget() {
		CACHE.clear();
	}

	public static void wrap(ModelEvent.ModifyBakingResult event) {
		for (BlockState state : ModBlocks.LIGHT_STRIP.get().getStateDefinition().getPossibleStates()) {
			ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
			BakedModel model = event.getModels().get(location);
			if (model != null && !(model instanceof StripModel))
				event.getModels().put(location, new StripModel(model));
		}
	}

	/** The lit diffuser (and an Edge Light's chamfer) takes its dye's (or warm) colour at full strength, darker by brightness step. */
	public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
		event.register((state, level, pos, tint) -> {
			if (tint != 0 || !state.hasProperty(LightBlock.LIGHT))
				return -1;
			int light = state.getValue(LightBlock.LIGHT);
			return LightRenderer.runGlow(state.getValue(LightBlock.COLOR).dye(), Math.max(0, light - 1));
		}, ModBlocks.LIGHT_STRIP.get(), ModBlocks.EDGE_LIGHT.get());
	}

	@Override
	public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
		StringBuilder key = new StringBuilder(state.getValue(LightBlock.LIGHT) > 0 ? "lit" : "dark");
		for (Direction face : Direction.values())
			key.append(';').append(LightStripBlock.hasFace(state, face) ? Integer.toString(StripLinks.links(level, pos, state, face)) : "x");
		return data.derive().with(STRIPS, key.toString()).build();
	}

	@Override
	public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
		return ChunkRenderTypeSet.of(RenderType.cutout());
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
		String key = data.get(STRIPS);
		if (key == null || side != null || (renderType != null && renderType != RenderType.cutout()))
			return List.of();
		return CACHE.computeIfAbsent(key, StripModel::build);
	}

	private static List<BakedQuad> build(String key) {
		String[] parts = key.split(";");
		boolean lit = parts[0].equals("lit");
		List<BakedQuad> out = new ArrayList<>();
		for (Direction face : Direction.values()) {
			String packed = parts[1 + face.ordinal()];
			if (packed.equals("x"))
				continue;
			int links = Integer.parseInt(packed);
			List<Direction> across = WireLink.across(face);
			boolean alongA = false, alongB = false;
			for (Direction d : across)
				if (WireLink.get(links, face, d).connected()) {
					if (d.getAxis() == across.get(0).getAxis())
						alongA = true;
					else
						alongB = true;
				}
			add(out, "centre", face, across.get(0), lit);
			if (alongA && alongB) {
				// a junction or a turn: an arm to each strip it meets
				for (Direction d : across)
					if (WireLink.get(links, face, d).connected())
						arm(out, face, d, WireLink.get(links, face, d), lit);
				continue;
			}
			// one way (or none yet): straight through the block, capped at the ends nothing meets
			// (the four directions across a face come in two pairs, one axis each)
			Direction.Axis axis = alongB ? across.get(2).getAxis() : across.get(0).getAxis();
			for (Direction d : across)
				if (d.getAxis() == axis)
					arm(out, face, d, WireLink.get(links, face, d), lit);
		}
		return out;
	}

	private static void arm(List<BakedQuad> out, Direction face, Direction d, WireLink link, boolean lit) {
		switch (link) {
			// up the next face of this block: stop short; the face that comes first draws the corner between them
			case INSIDE -> {
				add(out, "arm_inner", face, d, lit);
				if (face.ordinal() < d.ordinal())
					add(out, "corner", face, d, lit);
			}
			case OUTSIDE -> {
				add(out, "arm", face, d, lit);
				add(out, "fold", face, d, lit);
			}
			case LINE -> add(out, "arm", face, d, lit);
			default -> {
				add(out, "arm", face, d, lit);
				add(out, "cap", face, d, lit);
			}
		}
	}

	/** A part's channel and, if it has one, its diffuser (lit or unlit), turned from the floor running north onto face, running d. */
	private static void add(List<BakedQuad> out, String part, Direction face, Direction d, boolean lit) {
		turned(out, part(part), face, d);
		if (LIT.contains(part))
			turned(out, part(part + (lit ? "_glow" : "_glass")), face, d);
	}

	private static void turned(List<BakedQuad> out, ModelResourceLocation location, Direction face, Direction d) {
		BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
		List<BakedQuad> quads = new ArrayList<>();
		RandomSource rand = RandomSource.create(42);
		for (Direction s : Direction.values())
			quads.addAll(model.getQuads(null, s, rand, ModelData.EMPTY, null));
		quads.addAll(model.getQuads(null, null, rand, ModelData.EMPTY, null));
		Matrix3f m = PowerLineModel.rotation(face, d);
		// the turned quads face their new way, so they're shaded for it
		for (BakedQuad q : QuadTransformers.applying(PowerLineModel.turn(m)).process(quads)) {
			Vector3f n = m.transform(new Vector3f(q.getDirection().getStepX(), q.getDirection().getStepY(), q.getDirection().getStepZ()));
			Direction facing = Direction.getNearest(n.x, n.y, n.z);
			out.add(new BakedQuad(q.getVertices(), q.getTintIndex(), facing, q.getSprite(), q.isShade(), q.hasAmbientOcclusion()));
		}
	}
}
