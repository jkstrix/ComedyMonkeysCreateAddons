package com.comedymonkey.createaddons.content.chest;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.registry.ModBlocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;

/**
 * Turning chests into upgraded chests and back, single or double. A chest - single or double - holds exactly one
 * upgrade item, kept by one of its halves (see {@link UpgradedChestBlockEntity#holdsUpgrade()}). All server side.
 */
public final class ChestUpgrades {
	/** Update neighbours and clients, but don't let the half-converted pair re-evaluate its connection. */
	private static final int PAIR_FLAGS = Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE;

	private ChestUpgrades() {}

	/**
	 * The positions making up the chest at {@code pos}: just {@code pos}, or {@code pos} and its partner for a
	 * connected double chest. Works for vanilla and upgraded chests (they share the facing and type properties).
	 */
	public static List<BlockPos> halves(Level level, BlockPos pos, BlockState state) {
		if (!state.hasProperty(BlockStateProperties.CHEST_TYPE)
			|| state.getValue(BlockStateProperties.CHEST_TYPE) == ChestType.SINGLE)
			return List.of(pos);
		BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
		BlockState otherState = level.getBlockState(other);
		boolean paired = otherState.is(state.getBlock())
			&& otherState.getValue(BlockStateProperties.CHEST_TYPE) == state.getValue(BlockStateProperties.CHEST_TYPE).getOpposite()
			&& otherState.getValue(BlockStateProperties.HORIZONTAL_FACING) == state.getValue(BlockStateProperties.HORIZONTAL_FACING);
		return paired ? List.of(pos, other) : List.of(pos);
	}

	/** Vanilla chest (both halves of a double) -> upgraded chest of the given tier, using one upgrade. */
	static void upgradeVanillaChest(Level level, BlockPos pos, BlockState state, ChestTier tier, Player player,
									ItemStack upgrade) {
		List<BlockPos> parts = halves(level, pos, state);
		for (BlockPos part : parts) {
			BlockState partState = level.getBlockState(part);
			if (!(level.getBlockEntity(part) instanceof ChestBlockEntity chest))
				continue;
			chest.unpackLootTable(player);
			NonNullList<ItemStack> contents = NonNullList.withSize(ChestStacks.SLOTS, ItemStack.EMPTY);
			for (int i = 0; i < Math.min(chest.getContainerSize(), ChestStacks.SLOTS); i++)
				contents.set(i, chest.getItem(i).copy());
			// Empty the old chest first so it drops nothing when replaced
			chest.clearContent();

			level.setBlock(part, ModBlocks.UPGRADED_CHEST.get().defaultBlockState()
				.setValue(UpgradedChestBlock.FACING, partState.getValue(ChestBlock.FACING))
				.setValue(UpgradedChestBlock.TYPE, partState.getValue(ChestBlock.TYPE))
				.setValue(UpgradedChestBlock.TIER, tier), PAIR_FLAGS);
			if (level.getBlockEntity(part) instanceof UpgradedChestBlockEntity upgraded) {
				upgraded.fill(contents);
				// The clicked half keeps the one upgrade
				upgraded.setHoldsUpgrade(part.equals(pos));
			}
		}

		upgrade.consume(1, player);
		feedback(level, pos, player, tier);
	}

	/** Upgraded chest -> higher tier (both halves). The previous upgrade goes back to the player. */
	static void upgradeTier(Level level, BlockPos pos, BlockState state, ChestTier tier, Player player,
							ItemStack upgrade) {
		ChestTier current = state.getValue(UpgradedChestBlock.TIER);
		if (!tier.isAbove(current)) {
			player.displayClientMessage(Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".chest.already_tier",
				current.chestName()).withStyle(ChatFormatting.GOLD), true);
			return;
		}
		if (!fitsEachPart(level, pos, tier.multiplier(), tier.slots())) {
			// The config can give a higher tier a smaller stack size than these stacks need
			player.displayClientMessage(Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".chest.too_full_for_tier",
				tier.chestName()).withStyle(ChatFormatting.RED), true);
			return;
		}
		setTier(level, pos, tier);
		upgrade.consume(1, player);
		player.getInventory().placeItemBackInInventory(new ItemStack(current.upgradeItem()));
		feedback(level, pos, player, tier);
	}

	/** Changes the tier of both halves without breaking their connection. */
	static void setTier(Level level, BlockPos pos, ChestTier tier) {
		for (BlockPos part : halves(level, pos, level.getBlockState(pos))) {
			BlockState partState = level.getBlockState(part);
			if (!(partState.getBlock() instanceof UpgradedChestBlock) || partState.getValue(UpgradedChestBlock.TIER) == tier)
				continue;
			// Fewer slots would crash an open screen, and more would shift a double chest's second half
			if (tier.slots() != partState.getValue(UpgradedChestBlock.TIER).slots()
				&& level.getBlockEntity(part) instanceof UpgradedChestBlockEntity chest)
				UpgradedStorage.closeScreens(level, chest);
			level.setBlock(part, partState.setValue(UpgradedChestBlock.TIER, tier), PAIR_FLAGS);
		}
	}

	/** Upgraded chest -> vanilla chest (both halves). Callers make sure every stack fits a normal chest first. */
	static void revertToVanilla(Level level, BlockPos pos) {
		for (BlockPos part : halves(level, pos, level.getBlockState(pos)))
			revertHalf(level, part, false);
		level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 0.9f);
	}

	/**
	 * One half back to a vanilla chest, keeping its double-chest type. With {@code spillOverflow}, anything that
	 * doesn't fit a vanilla chest (oversized stacks, Diamond's extra slots) is dropped rather than assumed to fit.
	 */
	private static void revertHalf(Level level, BlockPos part, boolean spillOverflow) {
		BlockState partState = level.getBlockState(part);
		if (!(partState.getBlock() instanceof UpgradedChestBlock)
			|| !(level.getBlockEntity(part) instanceof UpgradedChestBlockEntity upgraded))
			return;

		NonNullList<ItemStack> contents = NonNullList.withSize(ChestStacks.SLOTS, ItemStack.EMPTY);
		int next = 0;
		for (ItemStack stack : ChestStacks.copy(upgraded.getItems())) {
			while (!stack.isEmpty() && next < ChestStacks.SLOTS)
				contents.set(next++, stack.split(stack.getMaxStackSize()));
			if (!stack.isEmpty() && spillOverflow)
				Containers.dropItemStack(level, part.getX(), part.getY(), part.getZ(), stack);
		}
		upgraded.clearContent();
		// The caller has already given the upgrade back, so replacing the half mustn't drop it too
		upgraded.setHoldsUpgrade(false);

		level.setBlock(part, Blocks.CHEST.defaultBlockState()
			.setValue(ChestBlock.FACING, partState.getValue(UpgradedChestBlock.FACING))
			.setValue(ChestBlock.TYPE, partState.getValue(UpgradedChestBlock.TYPE)), PAIR_FLAGS);
		if (level.getBlockEntity(part) instanceof ChestBlockEntity chest)
			for (int i = 0; i < contents.size(); i++)
				chest.setItem(i, contents.get(i));
	}

	/**
	 * A half is being broken: if it held the chest's upgrade, hand it to the other half (which stays upgraded) or,
	 * if there is no other half, report that the upgrade should drop.
	 */
	static boolean passOnUpgrade(Level level, BlockPos pos, BlockState state, UpgradedChestBlockEntity breaking) {
		if (!breaking.holdsUpgrade())
			return false;
		for (BlockPos part : halves(level, pos, state))
			if (!part.equals(pos) && level.getBlockEntity(part) instanceof UpgradedChestBlockEntity other
				&& !other.holdsUpgrade()) {
				other.setHoldsUpgrade(true);
				return false;
			}
		return true;
	}

	/** Upgraded chests placed (not moved) since their last check: these may join a matching neighbour. */
	private static final Set<GlobalPos> JUST_PLACED = ConcurrentHashMap.newKeySet();

	static void markJustPlaced(Level level, BlockPos pos) {
		JUST_PLACED.add(GlobalPos.of(level.dimension(), pos.immutable()));
	}

	/**
	 * The chest is gone before its check ran (vanilla skips a scheduled tick once the block has changed), so forget
	 * it: a chest that arrives there later mustn't be taken for a freshly placed one and join its neighbour.
	 */
	static void forgetJustPlaced(Level level, BlockPos pos) {
		JUST_PLACED.remove(GlobalPos.of(level.dimension(), pos));
	}

	/**
	 * Scheduled check after a half lost or gained its partner: a half whose partner is gone becomes a single chest;
	 * a freshly placed single (e.g. a half put down with Carry On) joins a matching single next to it; and a single
	 * chest without the upgrade turns back into a vanilla chest, dropping whatever no longer fits.
	 */
	static void validateHalf(Level level, BlockPos pos) {
		boolean justPlaced = JUST_PLACED.remove(GlobalPos.of(level.dimension(), pos));
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof UpgradedChestBlock))
			return;
		if (state.getValue(UpgradedChestBlock.TYPE) != ChestType.SINGLE && halves(level, pos, state).size() == 1) {
			state = state.setValue(UpgradedChestBlock.TYPE, ChestType.SINGLE);
			level.setBlock(pos, state, Block.UPDATE_ALL);
		}
		if (justPlaced && state.getValue(UpgradedChestBlock.TYPE) == ChestType.SINGLE && joinNeighbour(level, pos, state))
			return;
		if (state.getValue(UpgradedChestBlock.TYPE) == ChestType.SINGLE
			&& level.getBlockEntity(pos) instanceof UpgradedChestBlockEntity chest && !chest.holdsUpgrade())
			revertHalf(level, pos, true);
	}

	/**
	 * Joins a single upgraded chest with a matching single (same tier and facing) beside it. The double keeps one
	 * upgrade; if both halves had one, the spare pops out. Returns true if they joined.
	 */
	private static boolean joinNeighbour(Level level, BlockPos pos, BlockState state) {
		Direction facing = state.getValue(UpgradedChestBlock.FACING);
		ChestTier tier = state.getValue(UpgradedChestBlock.TIER);
		for (Direction side : new Direction[] { facing.getClockWise(), facing.getCounterClockWise() }) {
			BlockPos otherPos = pos.relative(side);
			BlockState other = level.getBlockState(otherPos);
			if (!(other.getBlock() instanceof UpgradedChestBlock)
				|| other.getValue(UpgradedChestBlock.TYPE) != ChestType.SINGLE
				|| other.getValue(UpgradedChestBlock.FACING) != facing
				|| other.getValue(UpgradedChestBlock.TIER) != tier)
				continue;

			if (!(level.getBlockEntity(pos) instanceof UpgradedChestBlockEntity placed)
				|| !(level.getBlockEntity(otherPos) instanceof UpgradedChestBlockEntity existing)
				|| (!placed.holdsUpgrade() && !existing.holdsUpgrade()))
				continue;

			// LEFT halves pair clockwise of their facing (same rule as vanilla chests)
			ChestType type = side == facing.getClockWise() ? ChestType.LEFT : ChestType.RIGHT;
			level.setBlock(pos, state.setValue(UpgradedChestBlock.TYPE, type), PAIR_FLAGS);
			level.setBlock(otherPos, other.setValue(UpgradedChestBlock.TYPE, type.getOpposite()), PAIR_FLAGS);
			if (placed.holdsUpgrade() && existing.holdsUpgrade()) {
				placed.setHoldsUpgrade(false);
				Containers.dropItemStack(level, pos.getX(), pos.getY() + 1, pos.getZ(), new ItemStack(tier.upgradeItem()));
			}
			level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1f, 0.8f);
			return true;
		}
		return false;
	}

	/** Whether each part's contents (chest halves, or a barrel) would fit the given multiplier and slot count. */
	static boolean fitsEachPart(Level level, BlockPos pos, int multiplier, int slots) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof TieredStorageBlock block))
			return false;
		for (BlockPos part : block.parts(level, pos, state))
			if (level.getBlockEntity(part) instanceof UpgradedStorage storage
				&& !ChestStacks.fits(storage.getItems(), multiplier, slots))
				return false;
		return true;
	}

	/** Opens the right chest screen for a container: 3 or 6 rows, or the 12x9 one for a diamond double chest. */
	static void openChestMenu(ServerPlayer player, Container container, Component name, int multiplier, ChestTier tier) {
		int size = container.getContainerSize();
		if (size > LargeUpgradedChestMenu.SLOTS / 2) {
			player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new LargeUpgradedChestMenu(id, inventory, container,
				tier), name), buf -> {
				buf.writeVarInt(multiplier);
				buf.writeVarInt(tier.ordinal());
			});
			return;
		}
		int rows = size / 9;
		player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new UpgradedChestMenu(id, inventory, container, rows,
			tier), name), buf -> {
			buf.writeVarInt(multiplier);
			buf.writeVarInt(tier.ordinal());
			buf.writeVarInt(rows);
		});
	}

	/** Right-click: open the chest, as a double chest when paired. */
	static void openChest(ServerPlayer player, Level level, BlockPos pos, BlockState state) {
		if (!(level.getBlockEntity(pos) instanceof UpgradedChestBlockEntity self))
			return;
		List<BlockPos> parts = halves(level, pos, state);
		Container container = self;
		Component name = self.getDisplayName();
		if (parts.size() == 2 && level.getBlockEntity(parts.get(1)) instanceof UpgradedChestBlockEntity other) {
			// Same order as vanilla: the RIGHT half fills the top rows
			boolean selfFirst = state.getValue(UpgradedChestBlock.TYPE) == ChestType.RIGHT;
			container = new BigCompoundContainer(selfFirst ? self : other, selfFirst ? other : self, self::multiplier);
			name = Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".chest.large", self.getTier().chestName());
		}
		openChestMenu(player, container, name, self.multiplier(), self.getTier());
	}

	/** Wrench (or sneak + right-click with an upgrade) on an upgraded chest or barrel. */
	public static void openUpgradeGui(ServerPlayer player, BlockPos pos) {
		player.openMenu(new SimpleMenuProvider(
				(id, inventory, p) -> new ChestUpgradeMenu(id, inventory, pos),
				Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.chest_upgrade.title")),
			buf -> buf.writeBlockPos(pos));
	}

	/**
	 * Sneak + right-click the side of a single upgraded chest while holding a chest: places a new half there with the
	 * same tier, joined into a double chest. The chest's one upgrade covers both halves. Returns true if handled.
	 */
	public static boolean extendToDouble(Level level, BlockPos pos, Direction face, Player player, ItemStack chestStack) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof UpgradedChestBlock) || state.getValue(UpgradedChestBlock.TYPE) != ChestType.SINGLE)
			return false;
		Direction facing = state.getValue(UpgradedChestBlock.FACING);
		if (face != facing.getClockWise() && face != facing.getCounterClockWise())
			return false;
		BlockPos newPos = pos.relative(face);
		if (!level.getBlockState(newPos).canBeReplaced() || !player.mayUseItemAt(newPos, face, chestStack))
			return false;
		if (level.isClientSide)
			return true;

		// LEFT halves pair clockwise of their facing (same rule as vanilla chests)
		ChestType existingType = face == facing.getClockWise() ? ChestType.LEFT : ChestType.RIGHT;
		BlockSnapshot before = BlockSnapshot.create(level.dimension(), level, newPos);
		level.setBlock(newPos, state.setValue(UpgradedChestBlock.TYPE, existingType.getOpposite())
			.setValue(UpgradedChestBlock.OPEN, false), PAIR_FLAGS);
		if (level.getBlockEntity(newPos) instanceof UpgradedChestBlockEntity added)
			added.setHoldsUpgrade(false);
		// A placement like any other, so claim and protection mods can refuse it: then put back what was there
		if (EventHooks.onBlockPlace(player, before, face)) {
			before.restore(PAIR_FLAGS);
			return true;
		}
		level.setBlock(pos, state.setValue(UpgradedChestBlock.TYPE, existingType), PAIR_FLAGS);

		if (!player.getAbilities().instabuild)
			chestStack.shrink(1);
		level.playSound(null, newPos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1f, 0.8f);
		ModAdvancements.award(player, ModAdvancements.CHEST_EXTENDED);
		return true;
	}

	private static void feedback(Level level, BlockPos pos, Player player, ChestTier tier) {
		ModAdvancements.award(player, ModAdvancements.CHEST_UPGRADE, new ItemStack(tier.upgradeItem()), tier.ordinal());
		level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.6f, 1.2f);
		player.displayClientMessage(Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".chest.upgraded",
			tier.chestName(), tier.multiplier()).withStyle(ChatFormatting.GOLD), true);
	}
}
