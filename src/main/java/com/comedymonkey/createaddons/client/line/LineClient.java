package com.comedymonkey.createaddons.client.line;

import java.util.function.Function;

import com.comedymonkey.createaddons.content.line.LineBlock;
import com.comedymonkey.createaddons.content.line.LineBlockEntity;
import com.comedymonkey.createaddons.content.line.LineColors;
import com.comedymonkey.createaddons.content.line.LineTier;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModDataComponents;
import com.comedymonkey.createaddons.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.equipment.goggles.GogglesItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** The lines' client hooks: their colours (tier or dye), their encasing, and the goggles' view of hidden lines. */
public final class LineClient {
	/** Hidden lines show within this many blocks of a player wearing Engineer's Goggles. */
	private static final int OUTLINE_RANGE = 16;

	private LineClient() {}

	/** The line's body is tinted its tier's colour or its dye; an encased line takes its block's colours instead. */
	public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
		event.register((state, level, pos, tint) -> {
			if (level == null || pos == null || !(level.getBlockEntity(pos) instanceof LineBlockEntity be))
				return state.getBlock() instanceof LineBlock ? LineColors.tier(tierOf(state.getBlock())) : -1;
			if (be.encasing() != null)
				return event.getBlockColors().getColor(be.encasing(), level, pos, tint);
			return tint == 0 ? LineColors.of(be.tier(), be.dye()) : -1;
		}, lineBlocks());
	}

	public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		event.register((stack, tint) -> tint == 0 ? LineColors.of(tierOf(stack), stack.get(ModDataComponents.LINE_DYE.get())) : -1,
			ModItems.COPPER_FLUID_LINE.get(), ModItems.BRASS_FLUID_LINE.get(), ModItems.COPPER_POWER_LINE.get(), ModItems.BRASS_POWER_LINE.get());
	}

	private static LineTier tierOf(ItemStack stack) {
		return stack.is(ModItems.BRASS_FLUID_LINE.get()) || stack.is(ModItems.BRASS_POWER_LINE.get()) ? LineTier.BRASS : LineTier.COPPER;
	}

	private static LineTier tierOf(Block block) {
		return block == ModBlocks.BRASS_FLUID_LINE.get() || block == ModBlocks.BRASS_POWER_LINE.get() ? LineTier.BRASS : LineTier.COPPER;
	}

	private static Block[] lineBlocks() {
		return BuiltInRegistries.BLOCK.stream().filter(b -> b instanceof LineBlock).toArray(Block[]::new);
	}

	/** Wraps every line block model so an encased line draws as its block. */
	public static void wrapModels(ModelEvent.ModifyBakingResult event) {
		wrap(event, ModBlocks.COPPER_FLUID_LINE.get(), EncasedLineModel::new);
		wrap(event, ModBlocks.BRASS_FLUID_LINE.get(), EncasedLineModel::new);
		wrap(event, ModBlocks.COPPER_POWER_LINE.get(), PowerLineModel::new);
		wrap(event, ModBlocks.BRASS_POWER_LINE.get(), PowerLineModel::new);
	}

	static void wrap(ModelEvent.ModifyBakingResult event, Block block, Function<BakedModel, BakedModel> wrapper) {
		for (BlockState state : block.getStateDefinition().getPossibleStates()) {
			ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
			BakedModel model = event.getModels().get(location);
			if (model != null && !(model instanceof EncasedLineModel))
				event.getModels().put(location, wrapper.apply(model));
		}
	}

	/** Wearing Engineer's Goggles, every encased line nearby shows as a faint cyan outline through its encasing. */
	public static void drawHiddenLines(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || !GogglesItem.isWearingGoggles(mc.player))
			return;
		Vec3 cam = event.getCamera().getPosition();
		BlockPos centre = mc.player.blockPosition();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		VertexConsumer lines = buffers.getBuffer(RenderType.lines());
		PoseStack ms = event.getPoseStack();
		int cx = centre.getX() >> 4, cz = centre.getZ() >> 4;
		for (int x = cx - 1; x <= cx + 1; x++)
			for (int z = cz - 1; z <= cz + 1; z++) {
				LevelChunk chunk = mc.level.getChunkSource().getChunkNow(x, z);
				if (chunk == null)
					continue;
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (!(be instanceof LineBlockEntity line) || line.encasing() == null)
						continue;
					BlockPos pos = be.getBlockPos();
					if (pos.distSqr(centre) > OUTLINE_RANGE * OUTLINE_RANGE)
						continue;
					if (!(be.getBlockState().getBlock() instanceof LineBlock block))
						continue;
					VoxelShape shape = block.lineShape(be.getBlockState(), mc.level, pos);
					LevelRenderer.renderVoxelShape(ms, lines, shape, pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z,
						0x6f / 255f, 0xb7 / 255f, 0xc4 / 255f, 0.5f, true);
				}
			}
		buffers.endBatch(RenderType.lines());
	}
}
