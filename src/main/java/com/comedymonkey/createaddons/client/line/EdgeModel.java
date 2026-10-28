package com.comedymonkey.createaddons.client.line;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.light.EdgeLinks;
import com.comedymonkey.createaddons.content.light.LightBlock;
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
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * An Edge Light block's model, built from its edges (in its block entity) and how each meets what's beyond its ends
 * (EdgeLinks): the middle of each edge, and at each end a join (on along the next block's edge, or the corner it draws
 * where it meets another edge), nothing (where the other edge draws the corner), or a cap. Parts are authored on the
 * floor at the north edge running along x and turned onto each face and edge; the lit chamfer is full-bright and tinted
 * by dye and brightness (the strip's block colour), or the unlit glass. Baked into the world, as strips are.
 */
public class EdgeModel extends BakedModelWrapper<BakedModel> {
	private static final ModelProperty<String> EDGES = new ModelProperty<>();
	private static final String[] PARTS = {"mid", "join_e", "join_w", "cap_e", "cap_w"};
	private static final Map<String, List<BakedQuad>> CACHE = new ConcurrentHashMap<>();

	public EdgeModel(BakedModel original) {
		super(original);
	}

	private static ModelResourceLocation part(String name) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/edge_light/" + name));
	}

	public static void registerModels(ModelEvent.RegisterAdditional event) {
		for (String p : PARTS) {
			event.register(part(p));
			event.register(part(p + "_glow"));
			event.register(part(p + "_glass"));
		}
	}

	public static void forget() {
		CACHE.clear();
	}

	public static void wrap(ModelEvent.ModifyBakingResult event) {
		for (BlockState state : ModBlocks.EDGE_LIGHT.get().getStateDefinition().getPossibleStates()) {
			ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
			BakedModel model = event.getModels().get(location);
			if (model != null && !(model instanceof EdgeModel))
				event.getModels().put(location, new EdgeModel(model));
		}
	}

	@Override
	public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
		int edges = EdgeLinks.edgesAt(level, pos);
		StringBuilder key = new StringBuilder(state.getValue(LightBlock.LIGHT) > 0 ? "lit" : "dark");
		for (Direction face : Direction.values())
			for (Direction side : WireLink.across(face)) {
				if (!EdgeLinks.has(edges, face, side))
					continue;
				Direction east = EdgeLinks.east(face, side);
				key.append(';').append(face.ordinal()).append(',').append(side.ordinal()).append(',')
					.append(EdgeLinks.end(level, pos, edges, face, side, east).ordinal()).append(',')
					.append(EdgeLinks.end(level, pos, edges, face, side, east.getOpposite()).ordinal());
			}
		return data.derive().with(EDGES, key.toString()).build();
	}

	@Override
	public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
		return ChunkRenderTypeSet.of(RenderType.cutout());
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
		String key = data.get(EDGES);
		if (key == null || side != null || (renderType != null && renderType != RenderType.cutout()))
			return List.of();
		return CACHE.computeIfAbsent(key, EdgeModel::build);
	}

	private static List<BakedQuad> build(String key) {
		String[] parts = key.split(";");
		boolean lit = parts[0].equals("lit");
		List<BakedQuad> out = new ArrayList<>();
		for (int i = 1; i < parts.length; i++) {
			String[] e = parts[i].split(",");
			Direction face = Direction.values()[Integer.parseInt(e[0])], side = Direction.values()[Integer.parseInt(e[1])];
			add(out, "mid", face, side, lit);
			end(out, EdgeLinks.End.values()[Integer.parseInt(e[2])], "e", face, side, lit);
			end(out, EdgeLinks.End.values()[Integer.parseInt(e[3])], "w", face, side, lit);
		}
		return out;
	}

	private static void end(List<BakedQuad> out, EdgeLinks.End end, String which, Direction face, Direction side, boolean lit) {
		switch (end) {
			case LINE, CORNER -> add(out, "join_" + which, face, side, lit);
			case NONE -> add(out, "cap_" + which, face, side, lit);
			// the edge it meets draws the corner
			case MET -> {}
		}
	}

	/** A part's body and its chamfer (lit or unlit), turned from the floor's north edge onto face, along side. */
	private static void add(List<BakedQuad> out, String part, Direction face, Direction side, boolean lit) {
		turned(out, part(part), face, side);
		turned(out, part(part + (lit ? "_glow" : "_glass")), face, side);
	}

	private static void turned(List<BakedQuad> out, ModelResourceLocation location, Direction face, Direction side) {
		BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
		List<BakedQuad> quads = new ArrayList<>();
		RandomSource rand = RandomSource.create(42);
		for (Direction s : Direction.values())
			quads.addAll(model.getQuads(null, s, rand, ModelData.EMPTY, null));
		quads.addAll(model.getQuads(null, null, rand, ModelData.EMPTY, null));
		Matrix3f m = EdgeLinks.rotation(face, side);
		for (BakedQuad q : QuadTransformers.applying(PowerLineModel.turn(m)).process(quads)) {
			Vector3f n = m.transform(new Vector3f(q.getDirection().getStepX(), q.getDirection().getStepY(), q.getDirection().getStepZ()));
			Direction facing = Direction.getNearest(n.x, n.y, n.z);
			out.add(new BakedQuad(q.getVertices(), q.getTintIndex(), facing, q.getSprite(), q.isShade(), q.hasAmbientOcclusion()));
		}
	}
}
