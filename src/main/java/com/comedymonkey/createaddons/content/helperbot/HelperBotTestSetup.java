package com.comedymonkey.createaddons.content.helperbot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModDataComponents;
import com.mojang.brigadier.context.CommandContext;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.condition.ScheduledDelay;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.content.trains.station.StationBlockEntity;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * {@code /helperbot testsetup}: builds Helper Bot's test courses next to you (a dev and play-test aid, op only).
 * <ul>
 * <li>Trains, along +x: a straight line of track with a station at each end ("Helper Bot A" in the west, "Helper Bot
 * B" in the east), a two-way train at B with a Helper Bot behind the controls at each end and one riding along as a
 * passenger, and a chest at B holding a round-trip schedule. Station B is put into assembly mode and the train
 * assembled once Create has registered the stations.</li>
 * <li>Shop, to the south: a chest of goods, a packager with a stock link and a Stock Ticker on one network, and a
 * Helper Bot keeping shop beside the ticker.</li>
 * <li>Off duty, to the north: one Helper Bot in each colour, and one ducking under a roof.</li>
 * </ul>
 */
public final class HelperBotTestSetup {
	public static final String STATION_A = "Helper Bot A", STATION_B = "Helper Bot B";
	/** Track runs x 0..LENGTH; station A targets x = A_X, station B x = B_X (the train is built behind B). */
	private static final int LENGTH = 66, A_X = 8, B_X = 58;

	private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

	private record Pending(ServerLevel level, BlockPos pos, String name, boolean assemble, long until) {}

	private static final List<Pending> PENDING = new ArrayList<>();
	/** Station B's assembly: entered on one tick, the train assembled on a later one. */
	private static final List<Pending> ASSEMBLE_NEXT = new ArrayList<>();

	private HelperBotTestSetup() {}

	/** Nothing queued outlives its server (singleplayer: quitting before a station registers). */
	public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
		PENDING.clear();
		ASSEMBLE_NEXT.clear();
	}

	public static void register(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("helperbot").requires(source -> source.hasPermission(2))
			.then(Commands.literal("testsetup").executes(HelperBotTestSetup::build))
			// checks for the courses: hand every train the round trip, and report what each one is doing
			.then(Commands.literal("testtrain").then(Commands.literal("go").executes(context -> trains(context, true)))
				.then(Commands.literal("status").executes(context -> trains(context, false)))));
	}

	private static int build(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		ServerLevel level = source.getLevel();
		BlockPos o = BlockPos.containing(source.getPosition());
		clear(level, o.offset(-3, -1, -16), o.offset(LENGTH + 3, 7, 20));
		trains(level, o, LENGTH, A_X, B_X);
		shop(level, o.offset(0, 0, 12));
		offDuty(level, o.offset(0, 0, -10));
		source.sendSuccess(() -> Component.literal("Built Helper Bot's test courses: trains along +x (stations '" + STATION_A + "' and '"
			+ STATION_B + "', schedule in the chest at B), the shop to the south, off-duty bots to the north."), true);
		return 1;
	}

	private static int trains(CommandContext<CommandSourceStack> context, boolean go) {
		CommandSourceStack source = context.getSource();
		int n = 0;
		for (com.simibubi.create.content.trains.entity.Train train : com.simibubi.create.Create.RAILWAYS.trains.values()) {
			n++;
			if (go)
				train.runtime.setSchedule(Schedule.fromTag(source.registryAccess(),
					schedule(source.getLevel()).get(AllDataComponents.TRAIN_SCHEDULE)), false);
			var station = train.getCurrentStation();
			var dest = train.navigation.destination;
			String line = train.name.getString() + ": speed " + String.format("%.3f", train.speed) + ", at " + (station == null ? "-" : station.name)
				+ ", heading for " + (dest == null ? "-" : dest.name) + ", no path " + train.status.navigation + ", derailed " + train.derailed
				+ ", schedule " + (train.runtime.getSchedule() != null) + ", paused " + train.runtime.paused + ", conductors fwd "
				+ train.hasForwardConductor() + " back " + train.hasBackwardConductor() + ", double-ended " + train.doubleEnded;
			source.sendSuccess(() -> Component.literal(line), false);
		}
		if (n == 0)
			source.sendSuccess(() -> Component.literal("no trains"), false);
		return n;
	}

	// ---------- trains ----------

	/**
	 * The train course at {@code o}: track along +x to {@code length}, station A targeting {@code aX} and B {@code bX},
	 * and the train at B (its front bogey at {@code bX - 1}). Also used by the game tests, on a shorter line.
	 */
	public static void trains(ServerLevel level, BlockPos o, int length, int aX, int bX) {
		int A_X = aX, B_X = bX;
		BlockState track = with(block("create:track").defaultBlockState(), "shape", "xo");
		for (int x = 0; x <= length; x++)
			level.setBlock(o.offset(x, 0, 0), track, Block.UPDATE_ALL);
		// stations beside the track, one block north of it. B (direction 0, as in Create's own Ponder scene) takes trains
		// heading east, with the train standing to its west; A takes them heading west.
		station(level, o.offset(A_X, 0, -1), true, STATION_A, false);
		station(level, o.offset(B_X, 0, -1), false, STATION_B, true);

		// a two-way carriage behind B: Create scans for bogeys from the block past the station's own track block, so the
		// front bogey sits one block west of B's, the other 4 further back
		int f = B_X - 1;
		BlockState bogey = with(block("create:small_bogey").defaultBlockState(), "axis", "x");
		for (int x : new int[] { f, f - 4 })
			setWithNbt(level, o.offset(x, 1, 0), bogey, tag("create:bogey"));
		BlockState casing = block("create:railway_casing").defaultBlockState();
		for (int x = f - 5; x <= f + 1; x++)
			level.setBlock(o.offset(x, 2, 0), casing, Block.UPDATE_ALL);
		BlockState controls = block("create:controls").defaultBlockState();
		BlockState bot = ModBlocks.HELPER_BOT.get().defaultBlockState();
		// front: controls at the east end facing back at the bot behind them; rear: the same, mirrored
		level.setBlock(o.offset(f + 1, 3, 0), controls.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST), Block.UPDATE_ALL);
		placeBot(level, o.offset(f, 3, 0), bot.setValue(HelperBotBlock.FACING, Direction.EAST), DyeColor.RED);
		level.setBlock(o.offset(f - 5, 3, 0), controls.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), Block.UPDATE_ALL);
		placeBot(level, o.offset(f - 4, 3, 0), bot.setValue(HelperBotBlock.FACING, Direction.WEST), DyeColor.BLUE);
		// a passenger in the middle, and a seat for whoever comes along
		placeBot(level, o.offset(f - 2, 3, 0), bot.setValue(HelperBotBlock.FACING, Direction.NORTH), DyeColor.YELLOW);
		level.setBlock(o.offset(f - 1, 3, 0), block("create:brown_seat").defaultBlockState(), Block.UPDATE_ALL);
		// super glue over the whole carriage, as a player would: neighbouring blocks don't hold together on a train
		level.addFreshEntity(new com.simibubi.create.content.contraptions.glue.SuperGlueEntity(level,
			new net.minecraft.world.phys.AABB(o.offset(f - 5, 1, 0).getCenter(), o.offset(f + 1, 3, 0).getCenter()).inflate(0.5)));

		// the schedule: to A, wait 5 s, to B, wait 5 s, round and round
		BlockPos chest = o.offset(B_X + 2, 0, -2);
		level.setBlock(chest, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
		if (level.getBlockEntity(chest) instanceof Container c)
			c.setItem(0, schedule(level));
		sign(level, o.offset(B_X + 3, 0, -2), Direction.SOUTH, "Schedule in", "the chest:", "use it on the", "front bot");
		sign(level, o.offset(A_X, 0, -3), Direction.SOUTH, "Station", STATION_A, "", "");
	}

	private static void station(ServerLevel level, BlockPos pos, boolean positive, String name, boolean assemble) {
		CompoundTag nbt = tag("create:track_station");
		nbt.put("TargetTrack", NbtUtils.writeBlockPos(new BlockPos(0, 0, 1)));
		nbt.putBoolean("TargetDirection", positive);
		// as Create writes it for a station on straight track along x
		nbt.putBoolean("Ortho", true);
		net.minecraft.nbt.ListTag axis = new net.minecraft.nbt.ListTag();
		for (double v : new double[] { 1, 0, 0 })
			axis.add(net.minecraft.nbt.DoubleTag.valueOf(v));
		nbt.put("PrevAxis", axis);
		nbt.putUUID("Id", UUID.randomUUID());
		setWithNbt(level, pos, block("create:track_station").defaultBlockState(), nbt);
		PENDING.add(new Pending(level, pos, name, assemble, level.getGameTime() + 600));
	}

	private static ItemStack schedule(ServerLevel level) {
		Schedule schedule = new Schedule();
		schedule.cyclic = true;
		for (String station : List.of(STATION_A, STATION_B)) {
			DestinationInstruction go = new DestinationInstruction();
			go.getData().putString("Text", station);
			ScheduleEntry entry = new ScheduleEntry();
			entry.instruction = go;
			List<com.simibubi.create.content.trains.schedule.condition.ScheduleWaitCondition> wait = new ArrayList<>();
			wait.add(new ScheduledDelay());
			entry.conditions.add(wait);
			schedule.entries.add(entry);
		}
		ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "schedule")));
		stack.set(AllDataComponents.TRAIN_SCHEDULE, schedule.write(level.registryAccess()));
		return stack;
	}

	/** Names the stations once Create has registered them, then assembles the train at B. */
	public static void onLevelTick(LevelTickEvent.Post event) {
		if (!(event.getLevel() instanceof ServerLevel level) || PENDING.isEmpty() && ASSEMBLE_NEXT.isEmpty())
			return;
		for (Iterator<Pending> it = ASSEMBLE_NEXT.iterator(); it.hasNext();) {
			Pending next = it.next();
			if (next.level() != level)
				continue;
			BlockPos pos = next.pos();
			if (level.getBlockEntity(pos) instanceof StationBlockEntity station && station.isAssembling()) {
				station.assemble(UUID.randomUUID());
				// and out of assembly mode again, as closing the station's screen does: trains can't path into a station
				// that's still assembling
				station.exitAssemblyMode();
				LOGGER.info("Helper Bot test setup: assembled at {}? station now {}", pos, station.saveWithoutMetadata(level.registryAccess()));
			}
			it.remove();
		}
		for (Iterator<Pending> it = PENDING.iterator(); it.hasNext();) {
			Pending p = it.next();
			if (p.level() != level)
				continue;
			if (level.getGameTime() > p.until()) {
				LOGGER.warn("Helper Bot test setup: station {} at {} never registered with Create's track graph", p.name(), p.pos());
				it.remove();
				continue;
			}
			if (!(level.getBlockEntity(p.pos()) instanceof StationBlockEntity station) || station.getStation() == null)
				continue;
			station.updateName(p.name());
			boolean assembling = p.assemble() && station.enterAssemblyMode(null);
			LOGGER.info("Helper Bot test setup: station {} registered at {}{}", p.name(), p.pos(), p.assemble() ? assembling ? ", assembly mode on" : ", assembly mode refused" : "");
			if (assembling)
				ASSEMBLE_NEXT.add(p);
			it.remove();
		}
	}

	// ---------- shop ----------

	private static void shop(ServerLevel level, BlockPos o) {
		UUID network = UUID.randomUUID();
		// goods in a chest; a packager in front of it pulls from it, its stock link on top joins the network
		BlockPos chest = o.offset(0, 0, 1);
		level.setBlock(chest, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
		if (level.getBlockEntity(chest) instanceof Container c) {
			ItemStack[] goods = { new ItemStack(Items.IRON_INGOT, 64), new ItemStack(Items.BREAD, 32), new ItemStack(Items.COBBLESTONE, 64),
				new ItemStack(Items.OAK_LOG, 32), new ItemStack(Items.COPPER_INGOT, 48), new ItemStack(Items.APPLE, 16), new ItemStack(Items.TORCH, 64) };
			for (int i = 0; i < goods.length; i++)
				c.setItem(i, goods[i]);
		}
		Block packager = block("create:packager");
		setWithNbt(level, o, packager.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH), tag("create:packager"));
		Block link = block("create:stock_link");
		BlockState linkState = link.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR)
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
		CompoundTag linkNbt = tag("create:packager_link");
		linkNbt.putUUID("Freq", network);
		setWithNbt(level, o.above(), linkState, linkNbt);
		// the ticker on the same network, a counter, and the bot keeping shop beside it, facing the customers
		BlockPos ticker = o.offset(3, 0, 0);
		CompoundTag tickerNbt = tag("create:stock_ticker");
		tickerNbt.putUUID("Freq", network);
		setWithNbt(level, ticker, block("create:stock_ticker").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), tickerNbt);
		placeBot(level, ticker.east(), ModBlocks.HELPER_BOT.get().defaultBlockState().setValue(HelperBotBlock.FACING, Direction.SOUTH), DyeColor.GREEN);
		sign(level, o.offset(5, 0, 2), Direction.SOUTH, "Shop:", "use the bot", "or the ticker", "to order");
	}

	// ---------- off duty ----------

	private static void offDuty(ServerLevel level, BlockPos o) {
		BlockState bot = ModBlocks.HELPER_BOT.get().defaultBlockState().setValue(HelperBotBlock.FACING, Direction.SOUTH);
		placeBot(level, o, bot, null);
		DyeColor[] dyes = DyeColor.values();
		for (int i = 0; i < dyes.length; i++)
			placeBot(level, o.offset(2 + i * 2, 0, 0), bot, dyes[i]);
		// one ducking under a roof
		BlockPos ducked = o.offset(36, 0, 0);
		level.setBlock(ducked.above(), Blocks.SPRUCE_PLANKS.defaultBlockState(), Block.UPDATE_ALL);
		placeBot(level, ducked, bot.setValue(HelperBotBlock.DUCKED, true), DyeColor.ORANGE);
	}

	// ---------- helpers ----------

	private static void placeBot(ServerLevel level, BlockPos pos, BlockState state, DyeColor dye) {
		level.setBlock(pos, state, Block.UPDATE_ALL);
		if (dye != null && level.getBlockEntity(pos) instanceof HelperBotBlockEntity be) {
			CompoundTag nbt = new CompoundTag();
			nbt.putString(HelperBotBlockEntity.DYE, dye.getName());
			be.loadWithComponents(nbt, level.registryAccess());
			be.setChanged();
			level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
		}
	}

	private static void setWithNbt(ServerLevel level, BlockPos pos, BlockState state, CompoundTag nbt) {
		level.setBlock(pos, state, Block.UPDATE_ALL);
		BlockEntity be = level.getBlockEntity(pos);
		if (be != null) {
			be.loadWithComponents(nbt, level.registryAccess());
			be.setChanged();
			level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
		}
	}

	private static void sign(ServerLevel level, BlockPos pos, Direction facing, String... lines) {
		BlockState state = Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, facing == Direction.SOUTH ? 0 : 8);
		level.setBlock(pos, state, Block.UPDATE_ALL);
		if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
			SignText text = new SignText();
			for (int i = 0; i < Math.min(4, lines.length); i++)
				text = text.setMessage(i, Component.literal(lines[i]));
			sign.setText(text, true);
			sign.setChanged();
			level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
		}
	}

	private static void clear(ServerLevel level, BlockPos from, BlockPos to) {
		for (BlockPos pos : BlockPos.betweenClosed(from, to))
			level.setBlock(pos, pos.getY() == from.getY() ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
	}

	private static CompoundTag tag(String id) {
		CompoundTag nbt = new CompoundTag();
		nbt.putString("id", id);
		return nbt;
	}

	private static Block block(String id) {
		return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
	}

	/** A state with one property set by name (for Create's blocks, whose property classes aren't needed here). */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static BlockState with(BlockState state, String property, String value) {
		Property prop = state.getBlock().getStateDefinition().getProperty(property);
		return state.setValue(prop, (Comparable) prop.getValue(value).orElseThrow());
	}
}
