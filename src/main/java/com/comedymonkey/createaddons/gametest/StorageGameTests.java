package com.comedymonkey.createaddons.gametest;

import java.util.UUID;
import java.util.function.Consumer;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.QuickTransfer;
import com.comedymonkey.createaddons.content.chest.BigCompoundContainer;
import com.comedymonkey.createaddons.content.chest.ChestStacks;
import com.comedymonkey.createaddons.content.chest.ChestTier;
import com.comedymonkey.createaddons.content.chest.ChestUpgrades;
import com.comedymonkey.createaddons.content.chest.ContraptionUpgradeInteraction;
import com.comedymonkey.createaddons.content.chest.TieredStorageBlock;
import com.comedymonkey.createaddons.content.chest.UpgradedChestBlock;
import com.comedymonkey.createaddons.content.chest.UpgradedChestBlockEntity;
import com.comedymonkey.createaddons.content.chest.UpgradedChestMenu;
import com.comedymonkey.createaddons.content.harvester.AutoHarvesterBlockEntity;
import com.comedymonkey.createaddons.content.harvester.AutoHarvesterMenu;
import com.comedymonkey.createaddons.content.pen.AnimalPenBlockEntity;
import com.comedymonkey.createaddons.content.pen.AnimalPenMenu;
import com.comedymonkey.createaddons.content.pen.BottledAnimalItem;
import com.comedymonkey.createaddons.content.wellspring.WellspringBlockEntity;
import com.comedymonkey.createaddons.content.wellspring.WellspringMenu;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlockEntity;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Upgraded chests and the machine screens: the things a spectator, a claim or a docked Portable Storage Interface
 * mustn't be able to get round. Each builds what it needs on the empty Trade Bot course and checks it in one go. Run
 * them headless with {@code ./gradlew runGameTestServer}, or one in game with {@code /test run comedymonkeysaddons:<name>}.
 */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StorageGameTests {
	private static final String COURSE = "trade_bot_course";

	private StorageGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(StorageGameTests.class);
	}

	/** Quick moves (buttons and mouse wheel) do nothing for a spectator, and still work for anyone else. */
	@GameTest(template = COURSE)
	public static void spectatorsCantQuickMove(GameTestHelper helper) {
		UpgradedChestBlockEntity chest = chest(helper, new BlockPos(4, 1, 4), ChestTier.COPPER, ChestType.SINGLE);
		chest.setItem(0, new ItemStack(Items.COBBLESTONE, 32));

		FakePlayer spectator = player(helper, true);
		UpgradedChestMenu menu = open(spectator, chest, ChestTier.COPPER);
		QuickTransfer.handle(spectator, menu, QuickTransfer.Action.ALL_OUT, -1);
		QuickTransfer.handle(spectator, menu, QuickTransfer.Action.ONE_AWAY, 0);
		menu.removed(spectator);
		if (chest.getItem(0).getCount() != 32 || spectator.getInventory().countItem(Items.COBBLESTONE) != 0)
			helper.fail("a spectator took " + spectator.getInventory().countItem(Items.COBBLESTONE) + " cobblestone");

		FakePlayer player = player(helper, false);
		menu = open(player, chest, ChestTier.COPPER);
		QuickTransfer.handle(player, menu, QuickTransfer.Action.ONE_AWAY, 0);
		menu.removed(player);
		if (chest.getItem(0).getCount() != 31 || player.getInventory().countItem(Items.COBBLESTONE) != 1)
			helper.fail("a player's quick move didn't take one cobblestone");
		helper.succeed();
	}

	/**
	 * The Wellspring, Animal Pen and Auto Harvester screens find their block from what the block entity itself sends,
	 * which NeoForge writes before anything else on every path (a spectator's right-click on a Wellspring, another
	 * mod's openMenu), not a stand-in.
	 */
	@GameTest(template = COURSE)
	public static void machineScreensFindTheirBlock(GameTestHelper helper) {
		FakePlayer player = player(helper, false);
		helper.setBlock(new BlockPos(2, 1, 4), ModBlocks.WELLSPRING.get());
		helper.setBlock(new BlockPos(4, 1, 4), ModBlocks.ANIMAL_PEN.get());
		helper.setBlock(new BlockPos(6, 1, 4), ModBlocks.AUTO_HARVESTER.get());
		WellspringBlockEntity wellspring = helper.getBlockEntity(new BlockPos(2, 1, 4));
		AnimalPenBlockEntity pen = helper.getBlockEntity(new BlockPos(4, 1, 4));
		AutoHarvesterBlockEntity harvester = helper.getBlockEntity(new BlockPos(6, 1, 4));

		// What vanilla's spectator path opens: the block's own menu provider
		if (helper.getLevel().getBlockState(wellspring.getBlockPos())
			.getMenuProvider(helper.getLevel(), wellspring.getBlockPos()) != wellspring)
			helper.fail("the Wellspring isn't its own menu provider");

		RegistryFriendlyByteBuf buf = sent(helper, wellspring, player);
		if (new WellspringMenu(1, player.getInventory(), buf).getBlockEntity() != wellspring || buf.isReadable())
			helper.fail("the Wellspring screen didn't find its block");
		buf = sent(helper, pen, player);
		if (new AnimalPenMenu(1, player.getInventory(), buf).getBlockEntity() != pen || buf.isReadable())
			helper.fail("the Animal Pen screen didn't find its block");
		buf = sent(helper, harvester, player);
		if (new AutoHarvesterMenu(1, player.getInventory(), buf).getBlockEntity() != harvester || buf.isReadable())
			helper.fail("the Auto Harvester screen didn't find its block");
		helper.succeed();
	}

	/**
	 * Asking the bottle slot how much it takes, or clicking it with the same bottle on the cursor (which vanilla turns
	 * into setting the slot again), leaves the herd alone.
	 */
	@GameTest(template = COURSE)
	public static void penBottleSlotKeepsTheHerd(GameTestHelper helper) {
		helper.setBlock(new BlockPos(4, 1, 4), ModBlocks.ANIMAL_PEN.get());
		AnimalPenBlockEntity pen = helper.getBlockEntity(new BlockPos(4, 1, 4));
		Cow cow = EntityType.COW.create(helper.getLevel());
		ItemStack bottle = BottledAnimalItem.of(cow);
		pen.setPonderHerd(bottle, 3, 2);

		FakePlayer player = player(helper, false);
		AnimalPenMenu menu = new AnimalPenMenu(1, player.getInventory(), pen);
		Slot slot = menu.getSlot(0);
		menu.setCarried(bottle.copy());
		menu.clicked(0, 0, ClickType.PICKUP, player);
		if (pen.getAdults() != 3 || pen.getYoung() != 2 || !pen.hasAnimal())
			helper.fail("the herd went from 3 + 2 to " + pen.getAdults() + " + " + pen.getYoung());
		for (int i = 0; i < pen.getOutput().getSlots(); i++)
			if (!pen.getOutput().getStackInSlot(i).isEmpty())
				helper.fail("the pen butchered some of the herd: " + pen.getOutput().getStackInSlot(i));
		if (!ItemStack.isSameItemSameComponents(menu.getCarried(), bottle))
			helper.fail("the bottle on the cursor became " + menu.getCarried());
		helper.succeed();
	}

	/** Asking the planter slot how much it takes, or clicking it with the same seeds, keeps the crop's growth. */
	@GameTest(template = COURSE)
	public static void planterSlotKeepsTheGrowth(GameTestHelper helper) {
		helper.setBlock(new BlockPos(4, 1, 4), ModBlocks.AUTO_HARVESTER.get());
		AutoHarvesterBlockEntity harvester = helper.getBlockEntity(new BlockPos(4, 1, 4));
		harvester.setPonderPlant(new ItemStack(Items.WHEAT_SEEDS), 0.5f);
		float growth = harvester.getGrowth(0);

		FakePlayer player = player(helper, false);
		AutoHarvesterMenu menu = new AutoHarvesterMenu(1, player.getInventory(), harvester);
		Slot slot = menu.getSlot(0);
		menu.setCarried(new ItemStack(Items.WHEAT_SEEDS, 4));
		menu.clicked(0, 0, ClickType.PICKUP, player);
		if (!harvester.hasPlant() || harvester.getGrowth(0) != growth)
			helper.fail("growth went from " + growth + " to " + harvester.getGrowth(0));
		if (menu.getCarried().getCount() != 4)
			helper.fail("the seeds on the cursor became " + menu.getCarried());
		helper.succeed();
	}

	/** Upgrading a Gold double chest to Diamond (more slots per half) closes the screen someone has open on it. */
	@GameTest(template = COURSE)
	public static void doubleChestScreenClosesWhenSlotsGrow(GameTestHelper helper) {
		// Facing north, a LEFT half's partner is to its east
		UpgradedChestBlockEntity left = chest(helper, new BlockPos(4, 1, 4), ChestTier.GOLD, ChestType.LEFT);
		UpgradedChestBlockEntity right = chest(helper, new BlockPos(5, 1, 4), ChestTier.GOLD, ChestType.RIGHT);
		FakePlayer player = player(helper, false);
		helper.getLevel().addNewPlayer(player);
		try {
			UpgradedChestMenu menu = new UpgradedChestMenu(1, player.getInventory(),
				new BigCompoundContainer(right, left, right::multiplier), 6, ChestTier.GOLD);
			player.containerMenu = menu;
			BlockState state = left.getBlockState();
			((TieredStorageBlock) state.getBlock()).setTier(helper.getLevel(), left.getBlockPos(), ChestTier.DIAMOND);
			if (left.getTier() != ChestTier.DIAMOND || right.getTier() != ChestTier.DIAMOND)
				helper.fail("the chest didn't become Diamond");
			if (player.containerMenu != player.inventoryMenu)
				helper.fail("the Gold screen stayed open on the Diamond chest");
		} finally {
			helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
		}
		helper.succeed();
	}

	/**
	 * Automation's view of a double chest is kept between looks, but follows its halves: a half growing without a new
	 * block state (as /data can make it) and a half going away both show at once.
	 */
	@GameTest(template = COURSE)
	public static void doubleChestAutomationFollowsItsHalves(GameTestHelper helper) {
		UpgradedChestBlockEntity left = chest(helper, new BlockPos(4, 1, 4), ChestTier.GOLD, ChestType.LEFT);
		UpgradedChestBlockEntity right = chest(helper, new BlockPos(5, 1, 4), ChestTier.GOLD, ChestType.RIGHT);
		IItemHandler both = left.getAutomationHandler();
		if (both.getSlots() != 2 * ChestStacks.SLOTS)
			helper.fail("the double chest shows " + both.getSlots() + " slots");
		if (left.getAutomationHandler() != both)
			helper.fail("the double chest's handler wasn't kept");

		// the right half loads a stack in a slot past its 27, as a /data merge could
		NonNullList<ItemStack> items = NonNullList.withSize(2 * ChestStacks.SLOTS, ItemStack.EMPTY);
		items.set(40, new ItemStack(Items.COBBLESTONE, 5));
		CompoundTag tag = right.saveWithoutMetadata(helper.getLevel().registryAccess());
		tag.put("Items", ChestStacks.save(items, helper.getLevel().registryAccess()));
		right.loadWithComponents(tag, helper.getLevel().registryAccess());
		int grown = left.getAutomationHandler().getSlots();
		if (grown != ChestStacks.SLOTS + right.getContainerSize())
			helper.fail("after the right half grew the double chest shows " + grown + " slots");

		helper.setBlock(new BlockPos(5, 1, 4), Blocks.AIR);
		helper.runAfterDelay(2, () -> {
			int single = left.getAutomationHandler().getSlots();
			if (single != ChestStacks.SLOTS)
				helper.fail("with its other half gone the chest shows " + single + " slots");
			helper.succeed();
		});
	}

	/** Extending a chest into a double is a block placement a claim can refuse; refused, nothing changes. */
	@GameTest(template = COURSE)
	public static void extendingAChestRespectsClaims(GameTestHelper helper) {
		BlockPos at = new BlockPos(4, 1, 4), beside = new BlockPos(5, 1, 4);
		UpgradedChestBlockEntity chest = chest(helper, at, ChestTier.IRON, ChestType.SINGLE);
		FakePlayer player = player(helper, false);
		ItemStack chests = new ItemStack(Items.CHEST, 2);

		Consumer<BlockEvent.EntityPlaceEvent> claim = event -> {
			if (event.getEntity() == player)
				event.setCanceled(true);
		};
		NeoForge.EVENT_BUS.addListener(claim);
		try {
			ChestUpgrades.extendToDouble(helper.getLevel(), helper.absolutePos(at), Direction.EAST, player, chests);
		} finally {
			NeoForge.EVENT_BUS.unregister(claim);
		}
		helper.assertBlockPresent(Blocks.AIR, beside);
		if (chest.getBlockState().getValue(UpgradedChestBlock.TYPE) != ChestType.SINGLE || !chest.holdsUpgrade()
			|| chests.getCount() != 2)
			helper.fail("a refused extension still changed the chest or took a chest");
		helper.assertItemEntityCountIs(chest.getTier().upgradeItem(), at, 3, 0);

		// Allowed, it does extend
		ChestUpgrades.extendToDouble(helper.getLevel(), helper.absolutePos(at), Direction.EAST, player, chests);
		helper.assertBlockPresent(ModBlocks.UPGRADED_CHEST.get(), beside);
		if (chests.getCount() != 1)
			helper.fail("extending took " + (2 - chests.getCount()) + " chests");
		helper.succeed();
	}

	/**
	 * A chest on a contraption docked at a Portable Storage Interface, upgraded in place: what comes out through the
	 * interface afterwards plus what's left in the contraption is what was put in, not twice that.
	 */
	@GameTest(template = COURSE)
	public static void dockedInterfaceSeesTheUpgradedChest(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos chestPos = helper.absolutePos(new BlockPos(4, 2, 4));
		BlockPos movingPos = chestPos.south(), stationaryPos = chestPos.south(3);
		level.setBlock(chestPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH), 3);
		((ChestBlockEntity) level.getBlockEntity(chestPos)).setItem(0, new ItemStack(Items.COBBLESTONE, 10));
		Block psiBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "portable_storage_interface"));
		BlockState psi = psiBlock.defaultBlockState();
		level.setBlock(movingPos, psi.setValue(BlockStateProperties.FACING, Direction.SOUTH), 3);
		level.setBlock(stationaryPos, psi.setValue(BlockStateProperties.FACING, Direction.NORTH), 3);
		level.addFreshEntity(new SuperGlueEntity(level, new AABB(chestPos).minmax(new AABB(movingPos))));

		// The chest and its interface on a bearing contraption, as Create assembles one
		BearingContraption contraption = new BearingContraption(false, Direction.UP);
		try {
			if (!contraption.assemble(level, chestPos.below())) {
				helper.fail("the contraption didn't assemble");
				return;
			}
		} catch (AssemblyException e) {
			helper.fail("the contraption didn't assemble: " + e.getMessage());
			return;
		}
		contraption.removeBlocksFromWorld(level, BlockPos.ZERO);
		OrientedContraptionEntity entity = OrientedContraptionEntity.create(level, contraption, Direction.NORTH);
		entity.setPos(chestPos.getX() + .5, chestPos.getY(), chestPos.getZ() + .5);
		level.addFreshEntity(entity);

		try {
			// Docked, as the moving interface's behaviour leaves it
			for (var actor : contraption.getActors())
				if (actor.getLeft().state().is(psiBlock))
					actor.getRight().data.put("WorkingPos", NbtUtils.writeBlockPos(stationaryPos));
			PortableStorageInterfaceBlockEntity station = (PortableStorageInterfaceBlockEntity) level.getBlockEntity(stationaryPos);
			station.startTransferringTo(contraption, 2);

			FakePlayer player = player(helper, false);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ChestTier.COPPER.upgradeItem()));
			new ContraptionUpgradeInteraction().handlePlayerInteraction(player, InteractionHand.MAIN_HAND, BlockPos.ZERO, entity);
			if (!contraption.getBlocks().get(BlockPos.ZERO).state().is(ModBlocks.UPGRADED_CHEST.get()))
				helper.fail("the chest on the contraption wasn't upgraded");

			IItemHandler through = level.getCapability(Capabilities.ItemHandler.BLOCK, stationaryPos, null);
			int pulled = 0;
			for (int slot = 0; through != null && slot < through.getSlots(); slot++)
				pulled += through.extractItem(slot, 64, false).getCount();
			IItemHandler left = contraption.getStorage().getAllItems();
			int remaining = 0;
			for (int slot = 0; slot < left.getSlots(); slot++)
				remaining += left.getStackInSlot(slot).getCount();
			if (pulled != 10 || remaining != 0)
				helper.fail(pulled + " pulled through the interface and " + remaining + " left on the contraption, from 10");
		} finally {
			entity.discard();
		}
		helper.succeed();
	}

	// ---- helpers

	/** A stand-in player beside the test's blocks (not joined to the server, see TradeBotGameTests). */
	private static FakePlayer player(GameTestHelper helper, boolean spectator) {
		FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(),
			spectator ? "spectator" : "player")) {
			@Override
			public boolean isSpectator() {
				return spectator;
			}
		};
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 1, 6)));
		player.moveTo(at.x, at.y, at.z, 0, 0);
		return player;
	}

	/** An upgraded chest (or half) facing north; it holds the upgrade, as a freshly upgraded chest does. */
	private static UpgradedChestBlockEntity chest(GameTestHelper helper, BlockPos pos, ChestTier tier, ChestType type) {
		helper.setBlock(pos, ModBlocks.UPGRADED_CHEST.get().defaultBlockState()
			.setValue(UpgradedChestBlock.FACING, Direction.NORTH)
			.setValue(UpgradedChestBlock.TYPE, type)
			.setValue(UpgradedChestBlock.TIER, tier));
		return helper.getBlockEntity(pos);
	}

	/** The chest's screen, open for the player (a FakePlayer's openMenu opens nothing). */
	private static UpgradedChestMenu open(FakePlayer player, UpgradedChestBlockEntity chest, ChestTier tier) {
		UpgradedChestMenu menu = new UpgradedChestMenu(1, player.getInventory(), chest, chest.getContainerSize() / 9, tier);
		player.containerMenu = menu;
		return menu;
	}

	/** What the server sends a client opening this block entity's screen. */
	private static RegistryFriendlyByteBuf sent(GameTestHelper helper, net.minecraft.world.MenuProvider be, FakePlayer player) {
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
		AbstractContainerMenu menu = be.createMenu(1, player.getInventory(), player);
		be.writeClientSideData(menu, buf);
		return buf;
	}
}
