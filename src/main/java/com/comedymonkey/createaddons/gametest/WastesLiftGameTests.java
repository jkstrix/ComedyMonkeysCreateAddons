package com.comedymonkey.createaddons.gametest;

import java.util.List;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.wastes.CityLink;
import com.comedymonkey.createaddons.content.wastes.CityRecord;
import com.comedymonkey.createaddons.content.wastes.District;
import com.comedymonkey.createaddons.content.wastes.LiftBlock;
import com.comedymonkey.createaddons.content.wastes.LiftBlockEntity;
import com.comedymonkey.createaddons.content.wastes.LiftCallPlateBlock;
import com.comedymonkey.createaddons.content.wastes.LiftCallPlateBlockEntity;
import com.comedymonkey.createaddons.content.wastes.LiftDeck;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The city's Lifts (design/specs/clockwork-wastes.md, P49, D40): dark and still while their district sleeps (and
 * unbreakable, for contraptions too); once it wakes, the deck carries a player and a mob up to a call plate's landing
 * and back down to the floor; anything in its way (a block in the shaft going up, a mob underneath going down) stops it
 * where it is. Each test builds a 2 x 2 shaft 10 blocks tall with a floor and a landing 5 up, its own city.
 */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WastesLiftGameTests {
	private static final String COURSE = "battery_course";
	/** The drive (the shaft's head), the floor's top (the bottom landing) and the call plate's landing. */
	private static final BlockPos DRIVE = new BlockPos(4, 11, 4), PLATE = new BlockPos(4, 6, 1);
	private static final int BOTTOM = 1, LANDING = 6;
	private static final double EPS = 0.02;

	private WastesLiftGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(WastesLiftGameTests.class);
	}

	/** The shaft (x 4-5, z 4-5 under the drive, facing north), its floor, a wall with the landing's call plate. */
	private static LiftBlockEntity build(GameTestHelper helper) {
		for (BlockPos p : BlockPos.betweenClosed(1, 1, 0, 8, 11, 8))
			helper.setBlock(p, Blocks.AIR.defaultBlockState());
		for (BlockPos p : BlockPos.betweenClosed(1, 0, 0, 8, 0, 8))
			helper.setBlock(p, Blocks.STONE.defaultBlockState());
		helper.setBlock(DRIVE, ModWastes.LIFT.get().defaultBlockState().setValue(LiftBlock.FACING, Direction.NORTH));
		helper.setBlock(PLATE.north(), Blocks.STONE.defaultBlockState());
		helper.setBlock(PLATE, ModWastes.LIFT_CALL_PLATE.get().defaultBlockState().setValue(LiftCallPlateBlock.FACING, Direction.SOUTH));
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		CityRecord.of(helper.getLevel()).add(origin);
		for (BlockPos p : List.of(DRIVE, PLATE))
			if (!CityLink.bind(helper.getLevel(), helper.absolutePos(p), origin, District.RAIL_YARD))
				helper.fail("no lift block to bind at " + p);
		return (LiftBlockEntity) helper.getBlockEntity(DRIVE);
	}

	private static void wake(GameTestHelper helper) {
		CityRecord.of(helper.getLevel()).wake(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), District.RAIL_YARD, null);
	}

	private static LiftDeck deck(GameTestHelper helper, LiftBlockEntity lift) {
		LiftDeck deck = lift.deck();
		if (deck == null || deck.isRemoved())
			helper.fail("the lift has no deck");
		return deck;
	}

	/** World y of the shaft's levels. */
	private static double level(GameTestHelper helper, int y) {
		return helper.absolutePos(new BlockPos(0, y, 0)).getY();
	}

	/** A mock player standing on the deck, in the level (so the deck finds it). */
	private static Player rider(GameTestHelper helper, LiftDeck deck, double dx, double dz) {
		Player p = helper.makeMockPlayer(GameType.SURVIVAL);
		p.moveTo(deck.getX() + dx, deck.top(), deck.getZ() + dz);
		helper.getLevel().addFreshEntity(p);
		return p;
	}

	private static Pig pig(GameTestHelper helper, Vec3 at) {
		Pig pig = EntityType.PIG.create(helper.getLevel());
		pig.moveTo(at.x, at.y, at.z);
		pig.setNoAi(true);
		helper.getLevel().addFreshEntity(pig);
		return pig;
	}

	private static void clickPlate(GameTestHelper helper, Player player) {
		BlockPos at = helper.absolutePos(PLATE);
		helper.getLevel().getBlockState(at).useWithoutItem(helper.getLevel(), player,
			new BlockHitResult(Vec3.atCenterOf(at), Direction.SOUTH, at, false));
	}

	private static void near(GameTestHelper helper, String what, double y, double want) {
		if (Math.abs(y - want) > EPS)
			helper.fail(what + " at y " + y + ", not " + want);
	}

	// ---------------------------------------------------------------- dormant

	/** Its district asleep: locked (bedrock's hardness, so contraptions pass it by), and neither the post nor a plate moves it. */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void liftStaysStillWhileDormant(GameTestHelper helper) {
		LiftBlockEntity lift = build(helper);
		helper.runAtTickTime(3, () -> {
			LiftDeck deck = deck(helper, lift);
			near(helper, "the deck", deck.getY(), level(helper, BOTTOM));
			if (helper.getLevel().getBlockState(lift.getBlockPos()).getDestroySpeed(helper.getLevel(), lift.getBlockPos()) != -1
				|| helper.getBlockState(PLATE).getDestroySpeed(helper.getLevel(), helper.absolutePos(PLATE)) != -1)
				helper.fail("a dormant lift is breakable");
			Player rider = rider(helper, deck, 0, 0);
			lift.usePost(rider);
			clickPlate(helper, helper.makeMockPlayer(GameType.SURVIVAL));
			if (lift.isMoving())
				helper.fail("moved while its district sleeps");
		});
		helper.runAtTickTime(80, () -> {
			near(helper, "the dormant deck", deck(helper, lift).getY(), level(helper, BOTTOM));
			if (lift.isAwake())
				helper.fail("lit while its district sleeps");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- riding

	/**
	 * Woken: a player on the deck uses its post and rides up to the plate's landing, a pig with them; using it again takes
	 * them both back down to the floor. The deck's underside stops level with each landing.
	 */
	@GameTest(template = COURSE, timeoutTicks = 400)
	public static void liftCarriesAPlayerAndAMobUpAndDown(GameTestHelper helper) {
		LiftBlockEntity lift = build(helper);
		Player[] rider = new Player[1];
		Pig[] pig = new Pig[1];
		helper.runAtTickTime(2, () -> wake(helper));
		helper.runAtTickTime(6, () -> {
			if (!lift.isAwake() || helper.getBlockState(DRIVE).getValue(LiftBlock.LOCKED))
				helper.fail("didn't wake with its district");
			if (helper.getLevel().getBlockState(lift.getBlockPos()).getDestroySpeed(helper.getLevel(), lift.getBlockPos()) != LiftBlock.HARDNESS)
				helper.fail("woken, it isn't an ordinary block");
			LiftDeck deck = deck(helper, lift);
			rider[0] = rider(helper, deck, -0.4, -0.4);
			pig[0] = pig(helper, new Vec3(deck.getX() + 0.3, deck.top(), deck.getZ() + 0.4));
		});
		helper.runAtTickTime(10, () -> {
			if (!lift.usePost(rider[0]) || !lift.isMoving())
				helper.fail("the post didn't send it up");
		});
		// partway up, both are still on it
		helper.runAtTickTime(60, () -> {
			LiftDeck deck = deck(helper, lift);
			if (deck.getY() <= level(helper, BOTTOM) + 0.5)
				helper.fail("hardly moved in 2.5 s");
			near(helper, "the rider partway", rider[0].getY(), deck.top());
			near(helper, "the pig partway", pig[0].getY(), deck.top());
		});
		helper.runAtTickTime(130, () -> {
			LiftDeck deck = deck(helper, lift);
			if (lift.isMoving())
				helper.fail("still moving after its ride");
			near(helper, "the deck at the landing", deck.getY(), level(helper, LANDING));
			near(helper, "the rider at the landing", rider[0].getY(), level(helper, LANDING) + LiftBlockEntity.DECK);
			near(helper, "the pig at the landing", pig[0].getY(), level(helper, LANDING) + LiftBlockEntity.DECK);
			if (!lift.usePost(rider[0]))
				helper.fail("the post didn't send it back down");
		});
		helper.runAtTickTime(260, () -> {
			near(helper, "the deck back on the floor", deck(helper, lift).getY(), level(helper, BOTTOM));
			near(helper, "the rider back down", rider[0].getY(), level(helper, BOTTOM) + LiftBlockEntity.DECK);
			near(helper, "the pig back down", pig[0].getY(), level(helper, BOTTOM) + LiftBlockEntity.DECK);
			// and a landing's plate calls it up again
			clickPlate(helper, helper.makeMockPlayer(GameType.SURVIVAL));
			if (!lift.isMoving() || Math.abs(lift.target() - level(helper, LANDING)) > EPS)
				helper.fail("the call plate didn't call it to its landing");
			rider[0].discard();
			pig[0].discard();
			helper.succeed();
		});
	}

	/**
	 * D62: a call plate's lens is dark until it's pressed (its district awake or not), lit while its call waits, and dark
	 * again once the deck arrives; pressed with the deck already there, nothing is called and it stays dark.
	 */
	@GameTest(template = COURSE, timeoutTicks = 260)
	public static void callPlateLensLightsWhileItsCallWaits(GameTestHelper helper) {
		LiftBlockEntity lift = build(helper);
		LiftCallPlateBlockEntity plate = (LiftCallPlateBlockEntity) helper.getBlockEntity(PLATE);
		helper.runAtTickTime(2, () -> wake(helper));
		helper.runAtTickTime(6, () -> {
			if (!plate.isAwake() || plate.isCalling())
				helper.fail("woken and not pressed, the plate's lens should be dark");
			clickPlate(helper, helper.makeMockPlayer(GameType.SURVIVAL));
			if (!lift.isMoving() || !plate.isCalling())
				helper.fail("pressed, the deck should come and the lens be lit");
		});
		helper.runAtTickTime(60, () -> {
			if (!lift.isMoving() || !plate.isCalling())
				helper.fail("on its way, the lens should stay lit");
		});
		helper.runAtTickTime(140, () -> {
			if (lift.isMoving())
				helper.fail("the deck should have arrived");
			if (plate.isCalling())
				helper.fail("the deck arrived: the lens should be dark again");
			clickPlate(helper, helper.makeMockPlayer(GameType.SURVIVAL));
			if (lift.isMoving() || plate.isCalling())
				helper.fail("pressed with the deck already there, nothing should be called");
			helper.succeed();
		});
	}

	/**
	 * A real (server) player riding up moves itself (its client carries it), so the server only stops counting it as
	 * floating, or it would be kicked for flying after four seconds on a long ride. (A joined mock player can't be used
	 * here: the mods' login payloads refuse its mock client; this checks the hook the deck uses on its connection.)
	 */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void liftKeepsARidingPlayerFromCountingAsFloating(GameTestHelper helper) {
		var server = helper.getLevel().getServer();
		var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "lift-rider");
		var player = new net.minecraft.server.level.ServerPlayer(server, helper.getLevel(), profile, net.minecraft.server.level.ClientInformation.createDefault());
		var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
		new io.netty.channel.embedded.EmbeddedChannel(connection);
		new net.minecraft.server.network.ServerGamePacketListenerImpl(server, connection, player,
			net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false));
		var counter = (com.comedymonkey.createaddons.mixin.ServerGamePacketListenerImplAccessor) player.connection;
		counter.comedymonkeysaddons$setAboveGroundTickCount(50);
		LiftDeck.keepAloft(player);
		if (counter.comedymonkeysaddons$getAboveGroundTickCount() != 0)
			helper.fail("a rider still counts as floating: " + counter.comedymonkeysaddons$getAboveGroundTickCount());
		helper.succeed();
	}

	// ---------------------------------------------------------------- in the way

	/** A block across the shaft above it: the deck stops under it, its rider still on board, and stays stopped. */
	@GameTest(template = COURSE, timeoutTicks = 300)
	public static void liftStopsUnderABlockInTheShaft(GameTestHelper helper) {
		LiftBlockEntity lift = build(helper);
		Player[] rider = new Player[1];
		helper.runAtTickTime(2, () -> wake(helper));
		helper.runAtTickTime(8, () -> {
			// a block across one column of the shaft, above the deck
			helper.setBlock(new BlockPos(5, 4, 5), Blocks.STONE.defaultBlockState());
			rider[0] = rider(helper, deck(helper, lift), 0.3, 0.3);
			lift.rideTo(level(helper, LANDING));
		});
		helper.runAtTickTime(160, () -> {
			LiftDeck deck = deck(helper, lift);
			if (lift.isMoving())
				helper.fail("never stopped");
			// the rider's head (1.8 tall) meets the block at y 4: the deck stops with its top under 4 - 1.8
			double block = level(helper, 4);
			if (rider[0].getBoundingBox().maxY > block + 1e-3 || deck.top() > block)
				helper.fail("went into the block: deck top " + deck.top() + ", rider's head " + rider[0].getBoundingBox().maxY);
			if (deck.getY() < level(helper, BOTTOM) + 0.3)
				helper.fail("stopped too soon, at " + deck.getY());
			near(helper, "the rider", rider[0].getY(), deck.top());
			rider[0].discard();
			helper.succeed();
		});
	}

	/** A mob underneath it on the way down: it stops above it, and doesn't crush it. */
	@GameTest(template = COURSE, timeoutTicks = 300)
	public static void liftStopsAboveAMobUnderneath(GameTestHelper helper) {
		LiftBlockEntity lift = build(helper);
		Pig[] pig = new Pig[1];
		helper.runAtTickTime(2, () -> wake(helper));
		helper.runAtTickTime(6, () -> lift.rideTo(level(helper, LANDING)));
		helper.runAtTickTime(120, () -> {
			if (lift.isMoving())
				helper.fail("didn't reach the landing");
			LiftDeck deck = deck(helper, lift);
			pig[0] = pig(helper, new Vec3(deck.getX(), level(helper, BOTTOM), deck.getZ()));
			lift.rideTo(level(helper, BOTTOM));
		});
		helper.runAtTickTime(250, () -> {
			LiftDeck deck = deck(helper, lift);
			if (lift.isMoving())
				helper.fail("never stopped");
			AABB p = pig[0].getBoundingBox();
			if (deck.getY() < p.maxY - 1e-3)
				helper.fail("came down onto the pig: deck at " + deck.getY() + ", the pig's back at " + p.maxY);
			if (deck.getY() > p.maxY + 0.25)
				helper.fail("stopped well above the pig, at " + deck.getY());
			if (!pig[0].isAlive())
				helper.fail("crushed the pig");
			pig[0].discard();
			helper.succeed();
		});
	}
}
