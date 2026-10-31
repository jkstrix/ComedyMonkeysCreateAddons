package com.comedymonkey.createaddons.content.wastes;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.wastes.bot.Custodian;
import com.comedymonkey.createaddons.content.wastes.bot.WastesBots;
import com.comedymonkey.createaddons.registry.ModWastes;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /wastes testsetup}: a play-test district of the powered city next to you (a dev aid, op only, not in releases),
 * every block bound to a city whose origin is where you stood: a Substation with Derelict Cable to a street of three
 * Derelict Street Lamps and a Wall Lamp, a district vault and the heart vault, a district Sliding Door and a heart one,
 * a Conduit Pylon, the Derelict Star Reactor and a Star Reactor. The district is the Foundry; the heart's blocks are the
 * heart's. Then, for the nearest city: {@code /wastes wake <district|all>}, {@code /wastes heart <minutes>} (the heart's
 * star burns that long: every lamp lit), {@code /wastes custodian wake|die|reset}. Waking by power: a charged Battery
 * beside the Substation.
 * <p>
 * {@code /wastes testheart}: a small heart hall round you (the bots, phase 3): the Custodian kneeling on a dais in the
 * middle, a Conduit Pylon in each corner, two wall vents beside each (live once the heart wakes; each bursts once when
 * the Custodian calls its Scuttlers), the heart vault behind it, walls round it; and outside its south wall a bit
 * of the Foundry with a Substation, four dormant Scuttlers, a Sentry on the floor and one on the wall. Wake the
 * Foundry ({@code /wastes wake foundry}: its bots stir within 5 s; feed it with a charged Battery on the Substation or
 * {@code /wastes heart <minutes>}, or its Sentries fold), then all four ({@code /wastes wake all}: the Custodian rises).
 * {@code /wastes custodian wake} also wakes a Custodian kneeling after a reset.
 */
public final class WastesTestSetup {
	private WastesTestSetup() {}

	public static void register(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("wastes").requires(source -> source.hasPermission(2))
			.then(Commands.literal("testsetup").executes(WastesTestSetup::build))
			.then(Commands.literal("testheart").executes(WastesTestSetup::heartHall))
			.then(Commands.literal("testlift").executes(WastesTestSetup::liftShaft))
			.then(Commands.literal("buildcity").executes(c -> buildCity(c, 0))
				.then(Commands.argument("seed", com.mojang.brigadier.arguments.LongArgumentType.longArg())
					.executes(c -> buildCity(c, com.mojang.brigadier.arguments.LongArgumentType.getLong(c, "seed")))
					.then(Commands.literal("kit").executes(c -> buildCity(c, com.mojang.brigadier.arguments.LongArgumentType.getLong(c, "seed"), true)))))
			.then(Commands.literal("buildcatalogue").executes(WastesTestSetup::buildCatalogue))
			.then(Commands.literal("buildoutskirts").executes(c -> buildOutskirts(c, 0))
				.then(Commands.argument("seed", com.mojang.brigadier.arguments.LongArgumentType.longArg())
					.executes(c -> buildOutskirts(c, com.mojang.brigadier.arguments.LongArgumentType.getLong(c, "seed")))))
			.then(Commands.literal("wake").then(Commands.argument("district", StringArgumentType.word()).executes(WastesTestSetup::wake)))
			.then(Commands.literal("heart").then(Commands.argument("minutes", IntegerArgumentType.integer(0, 600)).executes(WastesTestSetup::heart)))
			.then(Commands.literal("custodian").then(Commands.argument("what", StringArgumentType.word()).executes(WastesTestSetup::custodian))));
	}

	private static int build(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		ServerLevel level = source.getLevel();
		BlockPos o = BlockPos.containing(source.getPosition());
		// clear the ground and lay Scorched End Stone
		for (BlockPos p : BlockPos.betweenClosed(o.offset(-1, 0, -2), o.offset(38, 5, 10)))
			level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		for (BlockPos p : BlockPos.betweenClosed(o.offset(-1, -1, -2), o.offset(38, -1, 10)))
			level.setBlock(p, ModWastes.SCORCHED_END_STONE.get().defaultBlockState(), Block.UPDATE_CLIENTS);

		// the Substation, facing the street, and its cable east along the street's edge
		BlockState sub = ModWastes.SUBSTATION.get().defaultBlockState().setValue(SubstationBlock.FACING, Direction.SOUTH);
		level.setBlock(o.offset(1, 0, 1), sub, Block.UPDATE_ALL);
		level.setBlock(o.offset(1, 1, 1), sub.setValue(SubstationBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
		for (int x = 1; x <= 12; x++)
			level.setBlock(o.offset(x, 0, 2), ModWastes.DERELICT_CABLE.get().defaultBlockState(), Block.UPDATE_ALL);
		// three lamp posts across the street, their crooks over the cable; the middle one snapped
		BlockState post = ModWastes.DERELICT_STREET_LAMP.get().defaultBlockState().setValue(DerelictStreetLampBlock.FACING, Direction.NORTH);
		for (int x : new int[] {3, 7, 11})
			for (int y = 0; y < 3; y++)
				level.setBlock(o.offset(x, y, 4), y == 2 && x == 7 ? post.setValue(DerelictStreetLampBlock.SNAPPED, true) : post, Block.UPDATE_ALL);
		// a wall with a lamp on it
		level.setBlock(o.offset(14, 1, 1), ModWastes.SEAMED_VOID_ALLOY_PLATE.get().defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(o.offset(14, 0, 1), ModWastes.VOID_ALLOY_PLATE.get().defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(o.offset(14, 1, 2), ModWastes.DERELICT_WALL_LAMP.get().defaultBlockState().setValue(DerelictWallLampBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
		// cable up the wall's east face, and a leaning post (D41) and a Charging Alcove behind the street
		for (int y = 0; y < 2; y++)
			level.setBlock(o.offset(15, y, 1), ModWastes.DERELICT_CABLE.get().defaultBlockState().setValue(DerelictCableBlock.SURFACE, Direction.WEST), Block.UPDATE_ALL);
		for (int y = 0; y < 2; y++)
			level.setBlock(o.offset(15, y, 1), Block.updateFromNeighbourShapes(level.getBlockState(o.offset(15, y, 1)), level, o.offset(15, y, 1)), Block.UPDATE_ALL);
		for (int y = 0; y < 3; y++)
			level.setBlock(o.offset(9, y, 6), post.setValue(DerelictStreetLampBlock.LEANING, true), Block.UPDATE_ALL);
		BlockState alcove = ModWastes.CHARGING_ALCOVE.get().defaultBlockState().setValue(ThreePartBlock.FACING, Direction.NORTH);
		for (int y = 0; y < 3; y++)
			level.setBlock(o.offset(13, y, 7), alcove.setValue(ThreePartBlock.PART, ThreePartBlock.Part.values()[y]), Block.UPDATE_ALL);
		// the vaults
		BlockState vault = ModWastes.CITY_VAULT.get().defaultBlockState().setValue(CityVaultBlock.FACING, Direction.SOUTH);
		level.setBlock(o.offset(16, 0, 1), vault, Block.UPDATE_ALL);
		level.setBlock(o.offset(18, 0, 1), vault.setValue(CityVaultBlock.HEART, true), Block.UPDATE_ALL);
		// the doors (column 1 is on the facing's clockwise side: west, facing south)
		door(level, o.offset(22, 0, 1), false);
		door(level, o.offset(26, 0, 1), true);
		// a pylon, the heart's reactor, a Star Reactor standing on the ground
		BlockState pylon = ModWastes.CONDUIT_PYLON.get().defaultBlockState();
		level.setBlock(o.offset(28, 0, 4), pylon, Block.UPDATE_ALL);
		level.setBlock(o.offset(28, 1, 4), pylon.setValue(ThreePartBlock.PART, ThreePartBlock.Part.MIDDLE), Block.UPDATE_ALL);
		level.setBlock(o.offset(28, 2, 4), pylon.setValue(ThreePartBlock.PART, ThreePartBlock.Part.TOP), Block.UPDATE_ALL);
		BlockPos reactor = o.offset(32, 0, 5);
		BlockState dsr = ModWastes.DERELICT_STAR_REACTOR.get().defaultBlockState();
		for (int p = 0; p < 27; p++)
			level.setBlock(DerelictStarReactorBlock.partAt(reactor, p), dsr.setValue(DerelictStarReactorBlock.PART, p), Block.UPDATE_ALL);
		level.setBlock(o.offset(36, 0, 4), ModWastes.STAR_REACTOR.get().defaultBlockState().setValue(StarReactorBlock.FACING, Direction.DOWN), Block.UPDATE_ALL);

		CityRecord.of(level).add(o);
		// bind it all: the street's blocks to the Foundry, the heart's to the heart
		CityLink.bindAll(level, net.minecraft.world.level.levelgen.structure.BoundingBox.fromCorners(o.offset(0, 0, 0), o.offset(15, 4, 8)), o, District.FOUNDRY);
		CityLink.bind(level, o.offset(16, 0, 1), o, District.FOUNDRY);
		CityLink.bindAll(level, net.minecraft.world.level.levelgen.structure.BoundingBox.fromCorners(o.offset(20, 0, 1), o.offset(22, 2, 1)), o, District.FOUNDRY);
		CityLink.bind(level, o.offset(18, 0, 1), o, District.HEART);
		CityLink.bindAll(level, net.minecraft.world.level.levelgen.structure.BoundingBox.fromCorners(o.offset(24, 0, 1), o.offset(26, 2, 1)), o, District.HEART);
		CityLink.bindAll(level, net.minecraft.world.level.levelgen.structure.BoundingBox.fromCorners(o.offset(28, 0, 4), o.offset(33, 3, 6)), o, District.HEART);
		source.sendSuccess(() -> Component.literal("Clockwork Wastes test district built; its city is at " + o.toShortString()
			+ ". Wake it: a charged Battery beside the Substation, or /wastes wake foundry; /wastes heart <minutes>; /wastes custodian wake|die."), true);
		return 1;
	}

	/**
	 * The heart hall: 25 x 25, walls 4 high, the dais in the middle, pylons in the corners, eight wall vents (two by each
	 * pylon), the vault behind the dais.
	 */
	private static int heartHall(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		ServerLevel level = source.getLevel();
		BlockPos o = BlockPos.containing(source.getPosition());
		BlockState plate = ModWastes.VOID_ALLOY_PLATE.get().defaultBlockState(), seamed = ModWastes.SEAMED_VOID_ALLOY_PLATE.get().defaultBlockState();
		for (BlockPos p : BlockPos.betweenClosed(o.offset(-12, 0, -12), o.offset(12, 8, 22)))
			level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		for (BlockPos p : BlockPos.betweenClosed(o.offset(-12, -1, -12), o.offset(12, -1, 22)))
			level.setBlock(p, ModWastes.SCORCHED_END_STONE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
		for (BlockPos p : BlockPos.betweenClosed(o.offset(-12, -1, -12), o.offset(12, -1, 12)))
			level.setBlock(p, plate, Block.UPDATE_CLIENTS);
		for (int x = -12; x <= 12; x++)
			for (int z = -12; z <= 12; z++)
				if (Math.abs(x) == 12 || Math.abs(z) == 12)
					for (int y = 0; y < 4; y++)
						level.setBlock(o.offset(x, y, z), y == 1 ? seamed : plate, Block.UPDATE_CLIENTS);
		// a doorway in the south wall
		for (int x = -1; x <= 1; x++)
			for (int y = 0; y < 3; y++)
				level.setBlock(o.offset(x, y, 12), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		// the dais, one block up
		for (int x = -1; x <= 1; x++)
			for (int z = -1; z <= 1; z++)
				level.setBlock(o.offset(x, 0, z), x == 0 && z == 0 ? seamed : plate, Block.UPDATE_CLIENTS);
		// four pylons in the corners
		BlockState pylon = ModWastes.CONDUIT_PYLON.get().defaultBlockState();
		java.util.List<BlockPos> pylons = new java.util.ArrayList<>();
		for (int[] c : new int[][] {{-8, -8}, {8, -8}, {-8, 8}, {8, 8}}) {
			BlockPos p = o.offset(c[0], 0, c[1]);
			level.setBlock(p, pylon, Block.UPDATE_ALL);
			level.setBlock(p.above(), pylon.setValue(ThreePartBlock.PART, ThreePartBlock.Part.MIDDLE), Block.UPDATE_ALL);
			level.setBlock(p.above(2), pylon.setValue(ThreePartBlock.PART, ThreePartBlock.Part.TOP), Block.UPDATE_ALL);
			pylons.add(p.immutable());
		}
		// eight wall vents (D60), two beside each pylon: in the walls either side of its corner, two blocks in from it, as
		// the heart-hall preview has them; each faces into the hall
		java.util.List<BlockPos> vents = new java.util.ArrayList<>();
		BlockState vent = ModWastes.WALL_VENT.get().defaultBlockState();
		for (BlockPos p : pylons) {
			int sx = Integer.signum(p.getX() - o.getX()), sz = Integer.signum(p.getZ() - o.getZ());
			BlockPos side = o.offset(sx * 12, 0, p.getZ() - o.getZ() - sz * 2), end = o.offset(p.getX() - o.getX() - sx * 2, 0, sz * 12);
			level.setBlock(side, vent.setValue(WallVentBlock.FACING, sx > 0 ? Direction.WEST : Direction.EAST), Block.UPDATE_ALL);
			level.setBlock(end, vent.setValue(WallVentBlock.FACING, sz > 0 ? Direction.NORTH : Direction.SOUTH), Block.UPDATE_ALL);
			vents.add(side);
			vents.add(end);
		}
		// the heart vault behind the dais (the Custodian faces south, the doorway)
		BlockPos vault = o.offset(0, 0, -10);
		level.setBlock(vault, ModWastes.CITY_VAULT.get().defaultBlockState().setValue(CityVaultBlock.FACING, Direction.SOUTH).setValue(CityVaultBlock.HEART, true), Block.UPDATE_ALL);
		CityRecord.of(level).add(o);
		for (BlockPos p : pylons)
			CityLink.bind(level, p, o, District.HEART);
		CityLink.bind(level, vault, o, District.HEART);
		for (BlockPos v : vents)
			CityLink.bind(level, v, o, District.HEART);
		WastesBots.placeCustodian(level, o.above(), 0, o, pylons, vault);

		// outside the doorway: a bit of the Foundry, its Substation and its bots, asleep
		BlockState sub = ModWastes.SUBSTATION.get().defaultBlockState().setValue(SubstationBlock.FACING, Direction.NORTH);
		level.setBlock(o.offset(6, 0, 20), sub, Block.UPDATE_ALL);
		level.setBlock(o.offset(6, 1, 20), sub.setValue(SubstationBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
		CityLink.bindAll(level, BoundingBox.fromCorners(o.offset(6, 0, 20), o.offset(6, 1, 20)), o, District.FOUNDRY);
		for (int i = 0; i < 4; i++) {
			BlockPos heap = o.offset(-6 + i * 2, 0, 18);
			level.setBlock(heap, ModWastes.SCRAP_HEAP.get().defaultBlockState(), Block.UPDATE_ALL);
			WastesBots.placeScuttler(level, Vec3.atBottomCenterOf(heap.north()), o, District.FOUNDRY);
		}
		WastesBots.placeSentry(level, o.offset(3, -1, 18), Direction.UP, o, District.FOUNDRY);
		WastesBots.placeSentry(level, o.offset(-5, 2, 12), Direction.SOUTH, o, District.FOUNDRY);
		source.sendSuccess(() -> Component.literal("Clockwork Wastes heart hall built; its city is at " + o.toShortString()
			+ ". /wastes wake foundry (its bots stir; feed it: a charged Battery on the Substation, or /wastes heart <minutes>), "
			+ "/wastes wake all (the Custodian rises), /wastes custodian wake|die|reset."), true);
		return 1;
	}

	/**
	 * A lift shaft beside you (P49, D40): a 2 x 2 shaft 13 blocks tall, open to the north, its Lift at the head, its floor at
	 * your feet's level and two landings above (5 and 10 up), each a void-alloy walkway on the shaft's north side with a
	 * wall behind and a call plate on it (and one at the floor). All of it is the Rail Yard's of a city at your feet: wake
	 * it with {@code /wastes wake rail_yard}.
	 */
	private static int liftShaft(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		ServerLevel level = source.getLevel();
		BlockPos o = BlockPos.containing(source.getPosition());
		// the shaft: x 2-3, z 2-3 from you; its Lift at the head, facing north (open to the north and south)
		for (BlockPos p : BlockPos.betweenClosed(o.offset(-2, 0, -3), o.offset(7, 14, 7)))
			level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		for (BlockPos p : BlockPos.betweenClosed(o.offset(-2, -1, -3), o.offset(7, -1, 7)))
			level.setBlock(p, ModWastes.SCORCHED_END_STONE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
		BlockPos drive = o.offset(2, 13, 2);
		level.setBlock(drive, ModWastes.LIFT.get().defaultBlockState().setValue(LiftBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
		BlockState plate = ModWastes.LIFT_CALL_PLATE.get().defaultBlockState().setValue(LiftCallPlateBlock.FACING, Direction.SOUTH);
		BlockState wall = ModWastes.VOID_ALLOY_PLATE.get().defaultBlockState();
		java.util.List<BlockPos> plates = new java.util.ArrayList<>();
		for (int landing : new int[] {0, 5, 10}) {
			// the walkway north of the shaft (none at the floor) and the wall behind it, a call plate on the wall
			if (landing > 0)
				for (BlockPos p : BlockPos.betweenClosed(o.offset(1, landing - 1, -2), o.offset(4, landing - 1, 1)))
					level.setBlock(p, wall, Block.UPDATE_ALL);
			for (BlockPos p : BlockPos.betweenClosed(o.offset(1, landing, -3), o.offset(4, landing + 2, -3)))
				level.setBlock(p, wall, Block.UPDATE_ALL);
			BlockPos at = o.offset(4, landing, -2);
			level.setBlock(at, plate, Block.UPDATE_ALL);
			plates.add(at);
		}
		CityRecord.of(level).add(o);
		CityLink.bind(level, drive, o, District.RAIL_YARD);
		for (BlockPos p : plates)
			CityLink.bind(level, p, o, District.RAIL_YARD);
		source.sendSuccess(() -> Component.literal("Clockwork Wastes lift shaft built; its city is at " + o.toShortString()
			+ " (the Rail Yard). /wastes wake rail_yard wakes it; stand on the deck and use its post (sneak: down), or use a call plate."), true);
		return 1;
	}

	/**
	 * {@code /wastes buildcity [seed]} (D66): the whole city, its heart where you stand, on an End Stone island of its own
	 * (test it in a void or End world), every block bound to its district and its bots asleep; then you're put on the north
	 * boulevard looking at the heart. The same plan world generation would build (seed 0 unless given). Wake it with
	 * {@code /wastes wake <district|all>}, or power a Substation; {@code /wastes heart <minutes>} lights every lamp.
	 */
	private static int buildCity(CommandContext<CommandSourceStack> context, long seed) {
		return buildCity(context, seed, com.comedymonkey.createaddons.content.wastes.city.CityLayout.BUILDING_KIT);
	}

	/** {@code /wastes buildcity <seed> kit}: the same city with the building kit's buildings (the swap, previewed before it's made: D71). */
	private static int buildCity(CommandContext<CommandSourceStack> context, long seed, boolean kit) {
		CommandSourceStack source = context.getSource();
		ServerLevel level = source.getLevel();
		BlockPos o = BlockPos.containing(source.getPosition());
		long start = System.nanoTime();
		var result = com.comedymonkey.createaddons.content.wastes.city.CityBuild.build(level, o, seed, true, kit);
		long ms = (System.nanoTime() - start) / 1_000_000;
		if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)
			player.teleportTo(level, o.getX() + 0.5, o.getY(), o.getZ() - 60 + 0.5, 0, 10);
		source.sendSuccess(() -> Component.literal("Clockwork city built (seed " + seed + ", " + result.ops() + " steps, " + ms + " ms); its heart is at "
			+ o.toShortString() + ". Foundry north-west, Workshops north-east, Rail Yard south-east, Quarters south-west. "
			+ "/wastes wake <district|all>, /wastes heart <minutes>, /wastes custodian wake|die|reset."), true);
		return 1;
	}

	/**
	 * {@code /wastes buildcatalogue} (D71): the building kit's catalogue (design/briefs/clockwork-wastes-buildings.md), every
	 * building type in its states on its own pad, five rows going north from you, fronts to the south; you're put at the
	 * first row's street. Every exhibit is bound to the catalogue's own city: {@code /wastes wake all} then
	 * {@code /wastes heart <minutes>} lights every glow. Build it in a void or End world (it lays its own island).
	 */
	private static int buildCatalogue(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		ServerLevel level = source.getLevel();
		BlockPos o = BlockPos.containing(source.getPosition());
		long start = System.nanoTime();
		var built = com.comedymonkey.createaddons.content.wastes.city.kit.CatalogueBuild.build(level, o, 0);
		long ms = (System.nanoTime() - start) / 1_000_000;
		if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)
			player.teleportTo(level, o.getX() + 6.5, o.getY(), o.getZ() + 14.5, 180, 0);
		source.sendSuccess(() -> Component.literal("Building kit catalogue built (" + built.placed().size() + " exhibits, " + built.plan().ops().size() + " steps, "
			+ ms + " ms): five rows going north from " + o.toShortString() + ", fronts to the south, a sign on each pad. /wastes wake all, then /wastes heart 60, "
			+ "lights them. Camera marks: wastes-catalogue.json in the game folder."), true);
		return 1;
	}

	/**
	 * {@code /wastes buildoutskirts [seed]} (D69): an outskirts-only site (one whose island doesn't fit a city, P46), its
	 * middle where you stand, on the test island so every wreck shows: the scattered wrecks, the siege ring, the war-frames
	 * and the resistance's cache holding its Elytra; you're put 12 blocks south of the cache, looking at it.
	 */
	private static int buildOutskirts(CommandContext<CommandSourceStack> context, long seed) {
		CommandSourceStack source = context.getSource();
		ServerLevel level = source.getLevel();
		BlockPos o = BlockPos.containing(source.getPosition());
		var result = com.comedymonkey.createaddons.content.wastes.city.CityBuild.buildOutskirts(level, o, seed, true);
		if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)
			player.teleportTo(level, o.getX() + 0.5, o.getY(), o.getZ() + 12.5, 180, 10);
		source.sendSuccess(() -> Component.literal("Clockwork outskirts-only site built (seed " + seed + ", " + result.ops()
			+ " steps); the resistance's cache, with its Elytra, is at " + o.toShortString() + "."), true);
		return 1;
	}

	private static void door(ServerLevel level, BlockPos controller, boolean heart) {
		BlockState door = ModWastes.SLIDING_DOOR.get().defaultBlockState().setValue(SlidingDoorBlock.FACING, Direction.SOUTH).setValue(SlidingDoorBlock.HEART, heart);
		for (int c = 0; c < 2; c++)
			for (int r = 0; r < 3; r++)
				level.setBlock(SlidingDoorBlock.part(controller, Direction.SOUTH, c, r), door.setValue(SlidingDoorBlock.COL, c).setValue(SlidingDoorBlock.ROW, r), Block.UPDATE_ALL);
	}

	/** The city nearest the command's source (within 256 blocks), or null. */
	@Nullable
	private static BlockPos nearest(CommandSourceStack source) {
		BlockPos at = BlockPos.containing(source.getPosition()), best = null;
		double bestD = 256 * 256;
		for (BlockPos origin : CityRecord.of(source.getLevel()).cities()) {
			double d = origin.distSqr(at);
			if (d <= bestD) {
				bestD = d;
				best = origin;
			}
		}
		return best;
	}

	private static int wake(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		BlockPos origin = nearest(source);
		if (origin == null) {
			source.sendFailure(Component.literal("No city within 256 blocks"));
			return 0;
		}
		String name = StringArgumentType.getString(context, "district");
		CityRecord record = CityRecord.of(source.getLevel());
		BlockPos from = BlockPos.containing(source.getPosition());
		if (name.equals("all")) {
			record.wakeAll(source.getLevel(), origin, from);
			return 1;
		}
		for (District d : District.values())
			if (d.getSerializedName().equals(name)) {
				record.wake(source.getLevel(), origin, d, from);
				return 1;
			}
		source.sendFailure(Component.literal("No district " + name + " (foundry, workshops, rail_yard, quarters, all)"));
		return 0;
	}

	private static int heart(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		BlockPos origin = nearest(source);
		if (origin == null)
			return 0;
		int minutes = IntegerArgumentType.getInteger(context, "minutes");
		long until = minutes == 0 ? 0 : source.getLevel().getGameTime() + minutes * 1200L;
		CityRecord.of(source.getLevel()).burnHeart(source.getLevel(), origin, until, BlockPos.containing(source.getPosition()));
		return 1;
	}

	private static int custodian(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		BlockPos origin = nearest(source);
		if (origin == null)
			return 0;
		CityRecord record = CityRecord.of(source.getLevel());
		// the city's Custodian itself, if one is loaded; the record alone otherwise
		Custodian custodian = WastesBots.loaded(source.getLevel(), Custodian.class, origin, c -> true).stream().findFirst().orElse(null);
		switch (StringArgumentType.getString(context, "what")) {
			case "wake" -> {
				if (custodian != null)
					custodian.wake();
				record.wakeCustodian(source.getLevel(), origin);
			}
			case "die" -> {
				if (custodian != null)
					custodian.kill();
				record.custodianDied(source.getLevel(), origin);
			}
			case "reset" -> {
				if (custodian != null)
					custodian.beginReset();
				else
					record.custodianReset(source.getLevel(), origin);
			}
			default -> {
				source.sendFailure(Component.literal("wake, die or reset"));
				return 0;
			}
		}
		return 1;
	}
}
