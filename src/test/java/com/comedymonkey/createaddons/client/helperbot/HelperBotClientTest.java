package com.comedymonkey.createaddons.client.helperbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.content.helperbot.HelperBotBlock;
import com.comedymonkey.createaddons.content.helperbot.HelperBotBlockEntity;
import com.comedymonkey.createaddons.content.helperbot.HelperBotMovement;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visualization.VisualEmbedding;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.part.ModelTree;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;

/**
 * Helper Bot's client paths that run without a game: his 17 render types and their own buffers, his one buffer per
 * draw, the contraption state his train drawing reads (colour from the captured block entity data, the server's
 * status), his culling box, and his riding visual (Flywheel on) built from Create's movement context.
 */
class HelperBotClientTest {
	private static List<DyeColor> dyesAndPlain() {
		List<DyeColor> dyes = new ArrayList<>(List.of(DyeColor.values()));
		dyes.add(null);
		return dyes;
	}

	/** 17 render types, all different, each his texture's entity cutout type (the very object vanilla hands out). */
	@Test
	void seventeenDistinctRenderTypes() {
		List<RenderType> types = HelperBotModels.RENDER_TYPES;
		assertEquals(17, types.size());
		java.util.Set<RenderType> distinct = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
		distinct.addAll(types);
		assertEquals(17, distinct.size());
		assertEquals(17, types.stream().map(RenderType::toString).distinct().count());
		for (DyeColor dye : dyesAndPlain()) {
			RenderType type = HelperBotModels.renderType(dye);
			assertSame(RenderType.entityCutoutNoCull(HelperBotModels.texture(dye)), type, String.valueOf(dye));
			assertTrue(types.stream().anyMatch(t -> t == type), "not in the list: " + dye);
		}
	}

	/** Every one of them gets a buffer of its own, and nothing else is registered. */
	@Test
	void everyRenderTypeGetsItsOwnBuffer() {
		SequencedMap<RenderType, ByteBufferBuilder> buffers = new LinkedHashMap<>();
		try {
			HelperBotClient.registerRenderBuffers(new RegisterRenderBuffersEvent(buffers));
			assertEquals(17, buffers.size());
			for (DyeColor dye : dyesAndPlain())
				assertTrue(buffers.containsKey(HelperBotModels.renderType(dye)), String.valueOf(dye));
		} finally {
			buffers.values().forEach(ByteBufferBuilder::close);
		}
	}

	/** A draw asks for one buffer, his dye's (so it batches on the registered buffer, irises and all). */
	@Test
	void aDrawUsesOneBufferInHisDye() {
		HelperBotRig rig = new HelperBotRig(HelperBotModels.layer().bakeRoot());
		rig.reset();
		HelperBotModels.Baked model = new HelperBotModels.Baked(rig.root, rig);
		for (DyeColor dye : dyesAndPlain()) {
			List<RenderType> asked = new ArrayList<>();
			HelperBotFakes.Recorder out = new HelperBotFakes.Recorder();
			HelperBotModels.draw(new PoseStack(), type -> {
				asked.add(type);
				return out;
			}, LightTexture.pack(5, 5), OverlayTexture.NO_OVERLAY, Direction.SOUTH, dye, model);
			assertEquals(List.of(HelperBotModels.renderType(dye)), asked);
			assertEquals(48, out.lit(LightTexture.FULL_BRIGHT));
		}
	}

	/** On a contraption: his colour is read once from the captured block entity data, with the server's last status. */
	@Test
	void contraptionStateReadsHisColourOnce() {
		for (DyeColor dye : dyesAndPlain()) {
			CompoundTag data = new CompoundTag();
			if (dye != null)
				data.putString(HelperBotBlockEntity.DYE, dye.getName());
			MovementContext context = context(data, Direction.WEST);
			context.data.putByte(HelperBotMovement.STATUS, (byte) (HelperBotMovement.PAUSED | HelperBotMovement.DERAILED));
			HelperBotClient.State st = HelperBotClient.state(context);
			assertEquals(dye, st.dye);
			assertTrue(st.situation.paused);
			assertTrue(st.situation.derailed);
			assertFalse(st.situation.manual);
			assertFalse(st.situation.noPath);
			// cached: the same state, and the data isn't read again
			data.putString(HelperBotBlockEntity.DYE, "magenta");
			assertSame(st, HelperBotClient.state(context));
			assertEquals(dye, HelperBotClient.state(context).dye);
		}
	}

	/** His culling box: a block and a half tall, half a block past every side; worked out once. */
	@Test
	void renderBoxCoversHisReach() {
		BlockPos pos = new BlockPos(10, 64, -3);
		HelperBotBlockEntity be = new HelperBotBlockEntity(pos, ModBlocks.HELPER_BOT.get().defaultBlockState());
		AABB box = HelperBotClient.renderBox(be);
		assertEquals(new AABB(9.5, 63.5, -3.5, 11.5, 66.5, -1.5), box);
		assertSame(box, HelperBotClient.renderBox(be));
		// he stands 1.5 blocks tall plus his cap, and his arms reach under half a block past his block: inside it
		assertTrue(box.maxY - pos.getY() >= 1.5 + 0.5 && box.minX <= pos.getX() - 0.5 && box.maxX >= pos.getX() + 1.5);
	}

	/**
	 * His riding visual (Flywheel on): his colour from the captured data, the contraption's own block light (as
	 * Create's actors take it) with his irises full bright, every bone where his renderer would draw it in the
	 * contraption, and everything let go when the visual is.
	 */
	@Test
	void ridingVisualIsMadeFromTheMovementContext() {
		Function<Material, ModelTree> realTrees = HelperBotInstances.trees;
		List<Material> asked = new ArrayList<>();
		HelperBotInstances.trees = material -> {
			asked.add(material);
			return HelperBotFakes.tree();
		};
		try {
			CompoundTag data = new CompoundTag();
			data.putString(HelperBotBlockEntity.DYE, DyeColor.PURPLE.getName());
			MovementContext context = context(data, Direction.EAST);
			HelperBotFakes.Provider provider = new HelperBotFakes.Provider();
			HelperBotActorVisual visual = new HelperBotActorVisual(visualization(provider), lightAt(11), context);
			// embedded in the contraption: his embedded material (world light, flat, no occlusion)
			assertEquals(List.of(HelperBotInstances.material(DyeColor.PURPLE, true)), asked);
			for (Map.Entry<String, TransformedInstance> e : provider.byBone.entrySet())
				assertEquals(e.getKey().startsWith("iris_glow_") ? LightTexture.FULL_BRIGHT : LightTexture.pack(11, 0), e.getValue().light, e.getKey());

			// posed straight away, from his animator, at the context's block and facing
			HelperBotRig rig = HelperBotRig.skeleton();
			visual.draw(40, 0.25f);
			HelperBotClient.state(context).animator.pose(rig, HelperBotClient.state(context).situation, 40, 0.25f,
				new com.comedymonkey.createaddons.content.tradebot.clip.ClipPlayback());
			Matrix4f place = HelperBotInstances.place(new Matrix4f(), 2, 1, -4, Direction.EAST);
			for (int i = 0; i < HelperBotRig.bones(); i++) {
				TransformedInstance instance = provider.byBone.get(HelperBotRig.name(i));
				if (instance == null)
					continue;
				assertEquals(HelperBotFakes.drawn(rig, i), provider.handle(instance).visible, HelperBotRig.name(i));
				if (HelperBotFakes.drawn(rig, i)) {
					Matrix4f expected = HelperBotFakes.boneMatrix(rig, i, place);
					for (int c = 0; c < 4; c++)
						for (int r = 0; r < 4; r++)
							assertEquals(expected.get(c, r), instance.pose.get(c, r), 1e-4f, HelperBotRig.name(i));
				}
			}
			visual.delete();
			for (TransformedInstance instance : provider.all)
				assertTrue(provider.handle(instance).deleted);
		} finally {
			HelperBotInstances.trees = realTrees;
		}
	}

	private static MovementContext context(CompoundTag data, Direction facing) {
		BlockState state = ModBlocks.HELPER_BOT.get().defaultBlockState().setValue(HelperBotBlock.FACING, facing);
		return new MovementContext(null, new StructureTemplate.StructureBlockInfo(new BlockPos(2, 1, -4), state, data), null);
	}

	private static VisualizationContext visualization(InstancerProvider provider) {
		return new VisualizationContext() {
			@Override
			public InstancerProvider instancerProvider() {
				return provider;
			}

			@Override
			public Vec3i renderOrigin() {
				return Vec3i.ZERO;
			}

			@Override
			public VisualEmbedding createEmbedding(Vec3i renderOrigin) {
				throw new UnsupportedOperationException();
			}
		};
	}

	/** A contraption's world with this block light everywhere. */
	private static BlockAndTintGetter lightAt(int blockLight) {
		return new BlockAndTintGetter() {
			@Override
			public int getBrightness(LightLayer layer, BlockPos pos) {
				return layer == LightLayer.BLOCK ? blockLight : 0;
			}

			@Override
			public float getShade(Direction direction, boolean shade) {
				return 1;
			}

			@Override
			public LevelLightEngine getLightEngine() {
				throw new UnsupportedOperationException();
			}

			@Override
			public int getBlockTint(BlockPos pos, ColorResolver resolver) {
				return -1;
			}

			@Override
			@Nullable
			public BlockEntity getBlockEntity(BlockPos pos) {
				return null;
			}

			@Override
			public BlockState getBlockState(BlockPos pos) {
				return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
			}

			@Override
			public FluidState getFluidState(BlockPos pos) {
				return net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState();
			}

			@Override
			public int getHeight() {
				return 384;
			}

			@Override
			public int getMinBuildHeight() {
				return -64;
			}
		};
	}
}
