package com.comedymonkey.createaddons.compat.pipez;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import de.maxhenkel.pipez.blocks.tileentity.PipeLogicTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** The mark a Pipez pipe's tick puts on its thread (for Pipez-only Blaze Burners) comes off however the tick ends. */
class PipeTickMarkTest {
	@Test
	void depthNeverGoesBelowNothing() {
		PipezBlazeBurnerCompat.exitPipeTick();
		assertFalse(PipezBlazeBurnerCompat.inPipeTick());
		PipezBlazeBurnerCompat.enterPipeTick();
		assertTrue(PipezBlazeBurnerCompat.inPipeTick());
		PipezBlazeBurnerCompat.exitPipeTick();
		assertFalse(PipezBlazeBurnerCompat.inPipeTick());
	}

	/** A pipe with no level throws as it ticks: the mark must still come off. */
	@Test
	void aTickThatThrowsStillTakesTheMarkOff() {
		PipeLogicTileEntity pipe = pipe();
		assumeTrue(Arrays.stream(PipeLogicTileEntity.class.getDeclaredMethods())
			.anyMatch(m -> m.getName().contains("comedymonkeysaddons")), "mixins aren't applied in this environment");
		boolean threw = false;
		try {
			pipe.tick();
		} catch (RuntimeException expected) {
			threw = true;  // no level
		}
		assertTrue(threw, "the tick was meant to fail");
		assertFalse(PipezBlazeBurnerCompat.inPipeTick());
	}

	private static PipeLogicTileEntity pipe() {
		Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("pipez", "item_pipe"));
		assumeTrue(block != Blocks.AIR, "Pipez isn't loaded");
		BlockState state = block.defaultBlockState();
		state = state.setValue((BooleanProperty) block.getStateDefinition().getProperty("has_data"), true);
		BlockEntity be = ((EntityBlock) block).newBlockEntity(BlockPos.ZERO, state);
		assertNotNull(be);
		return (PipeLogicTileEntity) be;
	}
}
