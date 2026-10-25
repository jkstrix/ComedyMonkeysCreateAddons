package com.comedymonkey.createaddons.content.tradebot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.testutil.TestConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.phys.Vec3;

/** Trade Bot's station and the data in his item, on a block entity with no level. */
class TradeBotStationBlockEntityTest {
	private static HolderLookup.Provider registries;

	@BeforeAll
	static void lookup() {
		registries = VanillaRegistries.createLookup();
	}

	@BeforeEach
	void config() {
		TestConfig.loadDefaults();
	}

	private static TradeBotStationBlockEntity station() {
		return new TradeBotStationBlockEntity(BlockPos.ZERO, ModBlocks.TRADE_BOT.get().defaultBlockState());
	}

	@Test
	void takesFeFromUnderneathOnly() {
		TradeBotStationBlockEntity station = station();
		assertNotNull(station.getEnergy(Direction.DOWN));
		for (Direction side : List.of(Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST))
			assertNull(station.getEnergy(side), side.toString());
		assertEquals(1000, station.getEnergy(Direction.DOWN).receiveEnergy(1000, false));
		assertEquals(0, station.getEnergy(Direction.DOWN).extractEnergy(1000, false), "nothing comes back out");
		assertEquals(100_000, station.getEnergy().getMaxEnergyStored());
	}

	@Test
	void savesItsOwnerBotLinksAndEnergy() {
		TradeBotStationBlockEntity station = station();
		UUID owner = UUID.randomUUID(), bot = UUID.randomUUID();
		station.claim(owner, bot, new TradeBotData(0, 0, 0, Map.of(), ItemContainerContents.EMPTY,
			List.of(new BlockPos(3, 0, 1), new BlockPos(-2, 1, 0)), Optional.of(new BlockPos(0, 0, 4))));
		station.getEnergy().generate(42_000);
		CompoundTag tag = new CompoundTag();
		station.write(tag, registries, false);
		TradeBotStationBlockEntity loaded = station();
		loaded.read(tag, registries, false);
		assertEquals(owner, loaded.getOwner());
		assertEquals(bot, loaded.getBotId());
		assertEquals(42_000, loaded.getEnergy().getEnergyStored());
		CompoundTag again = new CompoundTag();
		loaded.write(again, registries, false);
		assertEquals(tag, again, "links survive a save and load");
	}

	@Test
	void comparatorIsZeroWithoutABot() {
		assertEquals(TradeBotStationBlockEntity.SIGNAL_NONE, station().comparatorOutput());
	}

	@Test
	void itemDataSurvivesTheCodecs() {
		TradeBotData data = new TradeBotData(73_000, 1_234, 7, Map.of(UUID.randomUUID(), 12),
			ItemContainerContents.fromItems(List.of(new ItemStack(Items.WHEAT, 64), ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT, 5))),
			List.of(new BlockPos(1, 0, 2)), Optional.of(new BlockPos(0, 1, -3)), Map.of(UUID.randomUUID(), 4_800L));
		Tag tag = TradeBotData.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), data).getOrThrow();
		assertEquals(data, TradeBotData.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).getOrThrow());
		assertEquals(TradeBotData.EMPTY, TradeBotData.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), new CompoundTag()).getOrThrow());
	}

	@Test
	void heDocksInTheCradleFacingTheFront() {
		// 2 px up, 1.5 px toward the front from the block's middle
		Vec3 north = TradeBotStationBlock.dockPoint(BlockPos.ZERO, Direction.NORTH);
		assertEquals(new Vec3(0.5, 2 / 16.0, 0.5 - 1.5 / 16), north);
		Vec3 east = TradeBotStationBlock.dockPoint(BlockPos.ZERO, Direction.EAST);
		assertEquals(new Vec3(0.5 + 1.5 / 16, 2 / 16.0, 0.5), east);
	}
}
