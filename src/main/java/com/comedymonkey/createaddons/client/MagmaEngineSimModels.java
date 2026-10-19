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
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * With Create Simulated installed, the Magma Engine takes the shape of its Portable Engine: our model JSONs name
 * Simulated's as their parent, so the game loads the geometry from their jar, and we only ship our own texture. The
 * baked models for each facing are made here by turning the one baked model, since Simulated's files can't be
 * rotated through a blockstate of ours. Without Simulated the engine keeps its own model, and nothing here runs.
 */
public final class MagmaEngineSimModels {
	public static final String SIMULATED_ID = "simulated";
	public static final boolean LOADED = ModList.get().isLoaded(SIMULATED_ID);

	public static final Parts STANDARD = new Parts("magma_engine");
	/** The same shapes in the Supercharged engine's brass-trimmed texture. */
	public static final Parts SUPERCHARGED = new Parts("supercharged_magma_engine");

	/** One engine's Simulated-shaped models, named {@code <prefix>_sim...}. */
	public record Parts(ModelResourceLocation body, ModelResourceLocation bodyLit, ModelResourceLocation item,
						ModelResourceLocation hatchTop, ModelResourceLocation hatchBottom,
						ModelResourceLocation pipeLeft, ModelResourceLocation pipeRight,
						ModelResourceLocation outletLeft, ModelResourceLocation outletRight,
						// The same exhaust parts glowing hot, drawn full-bright while the engine burns
						ModelResourceLocation pipeLeftLit, ModelResourceLocation pipeRightLit,
						ModelResourceLocation outletLeftLit, ModelResourceLocation outletRightLit) {
		Parts(String prefix) {
			this(standalone(prefix + "_sim"), standalone(prefix + "_sim_lit"), standalone(prefix + "_sim_item"),
				standalone(prefix + "_sim_hatch_top"), standalone(prefix + "_sim_hatch_bottom"),
				standalone(prefix + "_sim_exhaust_pipe_left"), standalone(prefix + "_sim_exhaust_pipe_right"),
				standalone(prefix + "_sim_exhaust_outlet_left"), standalone(prefix + "_sim_exhaust_outlet_right"),
				standalone(prefix + "_sim_exhaust_pipe_left_lit"), standalone(prefix + "_sim_exhaust_pipe_right_lit"),
				standalone(prefix + "_sim_exhaust_outlet_left_lit"), standalone(prefix + "_sim_exhaust_outlet_right_lit"));
		}

		List<ModelResourceLocation> all() {
			return List.of(body, bodyLit, item, hatchTop, hatchBottom, pipeLeft, pipeRight, outletLeft, outletRight,
				pipeLeftLit, pipeRightLit, outletLeftLit, outletRightLit);
		}
	}

	public static Parts partsFor(BlockState state) {
		return state.getBlock() instanceof MagmaEngineBlock engine && engine.isSupercharged() ? SUPERCHARGED : STANDARD;
	}

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
		STANDARD.all().forEach(event::register);
		SUPERCHARGED.all().forEach(event::register);
	}

	/** Swaps in the Simulated-shaped body for every state of both engines, and their items. */
	public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
		swap(event.getModels(), ModBlocks.MAGMA_ENGINE, STANDARD);
		swap(event.getModels(), ModBlocks.SUPERCHARGED_MAGMA_ENGINE, SUPERCHARGED);
	}

	private static void swap(Map<ModelResourceLocation, BakedModel> models, DeferredBlock<MagmaEngineBlock> block,
							 Parts parts) {
		BakedModel body = models.get(parts.body());
		BakedModel lit = models.get(parts.bodyLit());
		BakedModel item = models.get(parts.item());
		if (body == null || lit == null || item == null)
			return;
		for (BlockState state : block.get().getStateDefinition().getPossibleStates()) {
			BakedModel base = state.getValue(MagmaEngineBlock.LIT) ? lit : body;
			models.put(BlockModelShaper.stateToModelLocation(state),
				new Rotated(base, yRotation(state.getValue(MagmaEngineBlock.FACING))));
		}
		models.put(ModelResourceLocation.inventory(block.getId()), item);
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
