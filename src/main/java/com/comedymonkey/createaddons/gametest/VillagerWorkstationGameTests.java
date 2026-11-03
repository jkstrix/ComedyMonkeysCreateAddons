package com.comedymonkey.createaddons.gametest;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.VillagerWorkstations;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Villagers giving up their workstations ({@link VillagerWorkstations}), on Trade Bot's empty course template. Each
 * runs in a batch of its own: villagers claim workstations from 48 blocks, so they'd take each other's.
 */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VillagerWorkstationGameTests {
	private static final String COURSE = "trade_bot_course";

	private VillagerWorkstationGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(VillagerWorkstationGameTests.class);
	}

	/**
	 * An unemployed villager takes the only lectern; made to give it up, it loses its profession (it never traded) and,
	 * with a second lectern set down further off, walks to that one and never back to the first.
	 */
	@GameTest(template = COURSE, timeoutTicks = 1600, batch = "villager_gives_up_its_workstation")
	public static void givesUpItsWorkstation(GameTestHelper helper) {
		floor(helper);
		BlockPos first = new BlockPos(4, 1, 4), second = new BlockPos(16, 1, 4);
		helper.setBlock(first, Blocks.LECTERN);
		Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(6, 1, 6));
		GlobalPos firstSite = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(first));
		GlobalPos secondSite = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(second));
		boolean[] gaveUp = { false };
		helper.onEachTick(() -> {
			if (gaveUp[0] && firstSite.equals(VillagerWorkstations.station(villager)))
				helper.fail("it went back to the lectern it gave up");
		});
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(firstSite.equals(villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).orElse(null)),
				"no lectern taken yet"))
			.thenExecute(() -> {
				helper.assertTrue(firstSite.equals(VillagerWorkstations.unassign(villager)), "it gave up the wrong workstation");
				gaveUp[0] = true;
				helper.assertTrue(VillagerWorkstations.station(villager) == null, "it still has a workstation");
				helper.assertFalse(VillagerWorkstations.claimed(helper.getLevel(), helper.absolutePos(first)), "the lectern is still claimed");
				helper.assertTrue(VillagerWorkstations.ignores(villager, helper.absolutePos(first)), "it isn't leaving the lectern alone");
				helper.setBlock(second, Blocks.LECTERN);
			})
			.thenWaitUntil(() -> helper.assertTrue(villager.getVillagerData().getProfession() == VillagerProfession.NONE, "it kept its profession"))
			.thenWaitUntil(() -> helper.assertTrue(secondSite.equals(villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).orElse(null)),
				"it hasn't taken the second lectern"))
			.thenSucceed();
	}

	/**
	 * A librarian sitting in a Create seat beside its lectern gives it up: the lectern's ticket comes back, so any
	 * villager could take it, and the librarian (it has traded) keeps its profession.
	 */
	@GameTest(template = COURSE, timeoutTicks = 100, batch = "villager_seated_gives_up_its_workstation")
	public static void seatedGivesUpItsWorkstation(GameTestHelper helper) {
		floor(helper);
		BlockPos seat = new BlockPos(4, 1, 4), lectern = new BlockPos(4, 1, 5);
		helper.setBlock(seat, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "red_seat")));
		helper.setBlock(lectern, Blocks.LECTERN);
		Villager villager = helper.spawn(EntityType.VILLAGER, seat);
		villager.setVillagerData(villager.getVillagerData().setProfession(VillagerProfession.LIBRARIAN).setLevel(1));
		villager.setVillagerXp(1);
		SeatBlock.sitDown(helper.getLevel(), helper.absolutePos(seat), villager);
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(lectern);
		helper.startSequence()
			// (the lectern's point of interest is added a tick after the block)
			.thenWaitUntil(() -> helper.assertTrue(level.getPoiManager().getType(at).isPresent(), "the lectern isn't a workstation yet"))
			.thenExecute(() -> {
				level.getPoiManager().take(VillagerProfession.LIBRARIAN.heldJobSite(), (type, pos) -> pos.equals(at), at, 1);
				villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(level.dimension(), at));
				helper.assertTrue(villager.isPassenger(), "it isn't sitting");
				helper.assertTrue(VillagerWorkstations.claimed(level, at), "the lectern isn't claimed");
				helper.assertTrue(VillagerWorkstations.owner(level, at) == villager, "the lectern's owner isn't the librarian");
				VillagerWorkstations.unassign(villager);
				helper.assertFalse(VillagerWorkstations.claimed(level, at), "the lectern is still claimed");
				helper.assertTrue(level.getPoiManager().getInRange(t -> true, at, 0, PoiManager.Occupancy.HAS_SPACE).findAny().isPresent(),
					"the lectern's ticket didn't come back");
				helper.assertTrue(VillagerWorkstations.owner(level, at) == null, "the librarian still holds the lectern");
			})
			.thenIdle(5)
			.thenExecute(() -> helper.assertTrue(villager.getVillagerData().getProfession() == VillagerProfession.LIBRARIAN,
				"a librarian that traded lost its profession"))
			.thenSucceed();
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 23; x++)
			for (int z = 0; z <= 23; z++)
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
	}
}
