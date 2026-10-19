package com.comedymonkey.createaddons.client;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.engine.MagmaEngineBlock;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.mojang.math.Transformation;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * With Create Simulated installed, the Magma Engine takes the shape of its Portable Engine: our model JSONs name
 * Simulated's as their parent, so the game loads the geometry from their jar, and we only ship our own texture. The
 * baked models for each facing are made here by turning the one baked model, since Simulated's files can't be
 * rotated through a blockstate of ours. Without Simulated the engine keeps its own model, and nothing here runs.
 */
public final class MagmaEngineSimModels {
	public static final String SIMULATED_ID = "simulated";
	public static final boolean LOADED = ModList.get().isLoaded(SIMULATED_ID);

	public static final ModelResourceLocation BODY = standalone("magma_engine_sim");
	public static final ModelResourceLocation BODY_LIT = standalone("magma_engine_sim_lit");
	public static final ModelResourceLocation ITEM = standalone("magma_engine_sim_item");
	public static final ModelResourceLocation HATCH_TOP = standalone("magma_engine_sim_hatch_top");
	public static final ModelResourceLocation HATCH_BOTTOM = standalone("magma_engine_sim_hatch_bottom");
	public static final ModelResourceLocation PIPE_LEFT = standalone("magma_engine_sim_exhaust_pipe_left");
	public static final ModelResourceLocation PIPE_RIGHT = standalone("magma_engine_sim_exhaust_pipe_right");
	public static final ModelResourceLocation OUTLET_LEFT = standalone("magma_engine_sim_exhaust_outlet_left");
	public static final ModelResourceLocation OUTLET_RIGHT = standalone("magma_engine_sim_exhaust_outlet_right");
	// The same exhaust parts glowing hot, drawn full-bright while the engine burns
	public static final ModelResourceLocation PIPE_LEFT_LIT = standalone("magma_engine_sim_exhaust_pipe_left_lit");
	public static final ModelResourceLocation PIPE_RIGHT_LIT = standalone("magma_engine_sim_exhaust_pipe_right_lit");
	public static final ModelResourceLocation OUTLET_LEFT_LIT = standalone("magma_engine_sim_exhaust_outlet_left_lit");
	public static final ModelResourceLocation OUTLET_RIGHT_LIT = standalone("magma_engine_sim_exhaust_outlet_right_lit");
	private static final List<ModelResourceLocation> ALL = List.of(BODY, BODY_LIT, ITEM, HATCH_TOP, HATCH_BOTTOM,
		PIPE_LEFT, PIPE_RIGHT, OUTLET_LEFT, OUTLET_RIGHT, PIPE_LEFT_LIT, PIPE_RIGHT_LIT, OUTLET_LEFT_LIT, OUTLET_RIGHT_LIT);

	private static ModelResourceLocation standalone(String name) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/" + name));
	}

	/**
	 * Simulated's engine is modelled with its hatch to the north. Ours puts the hatch at the back, opposite the shaft,
	 * so the model turns by the shaft's facing (as a blockstate "y" would).
	 */
	public static float yRotation(Direction facing) {
		return facing.toYRot();
	}

	public static void registerModels(ModelEvent.RegisterAdditional event) {
		ALL.forEach(event::register);
	}

	/** Swaps in the Simulated-shaped body for every state of the engine, and its item. */
	public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
		Map<ModelResourceLocation, BakedModel> models = event.getModels();
		BakedModel body = models.get(BODY);
		BakedModel lit = models.get(BODY_LIT);
		BakedModel item = models.get(ITEM);
		if (body == null || lit == null || item == null)
			return;
		for (BlockState state : ModBlocks.MAGMA_ENGINE.get().getStateDefinition().getPossibleStates()) {
			BakedModel base = state.getValue(MagmaEngineBlock.LIT) ? lit : body;
			models.put(BlockModelShaper.stateToModelLocation(state),
				new Rotated(base, yRotation(state.getValue(MagmaEngineBlock.FACING))));
		}
		models.put(ModelResourceLocation.inventory(ModBlocks.MAGMA_ENGINE.getId()), item);
	}

	/** A baked model turned about the block's vertical axis, the way a blockstate "y" rotation turns one. */
	private static final class Rotated extends BakedModelWrapper<BakedModel> {
		private final Matrix4f matrix;
		private final Matrix4f inverse;
		private final IQuadTransformer transformer;
		private final Map<RenderType, Map<Direction, List<BakedQuad>>> cache = new java.util.HashMap<>();
		private final Map<RenderType, List<BakedQuad>> unculled = new java.util.HashMap<>();

		Rotated(BakedModel original, float degrees) {
			super(original);
			// Blockstate y rotations turn clockwise seen from above, about the block centre
			matrix = new Matrix4f().translate(0.5f, 0.5f, 0.5f)
				.rotateY((float) Math.toRadians(-degrees))
				.translate(-0.5f, -0.5f, -0.5f);
			inverse = new Matrix4f(matrix).invert();
			transformer = QuadTransformers.applying(new Transformation(matrix));
		}

		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
			return getQuads(state, side, rand, ModelData.EMPTY, null);
		}

		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
										ModelData data, @Nullable RenderType renderType) {
			if (side == null)
				return unculled.computeIfAbsent(renderType, t -> turn(originalModel.getQuads(state, null, rand, data, t)));
			return cache.computeIfAbsent(renderType, t -> new EnumMap<>(Direction.class))
				.computeIfAbsent(side, s -> turn(originalModel.getQuads(state, Direction.rotate(inverse, s), rand, data, renderType)));
		}

		private List<BakedQuad> turn(List<BakedQuad> quads) {
			return quads.stream().map(quad -> {
				BakedQuad moved = transformer.process(quad);
				return new BakedQuad(Arrays.copyOf(moved.getVertices(), moved.getVertices().length), quad.getTintIndex(),
					Direction.rotate(matrix, quad.getDirection()), quad.getSprite(), quad.isShade());
			}).toList();
		}
	}

	private MagmaEngineSimModels() {}
}
