package com.comedymonkey.createaddons.content.tradebot.work;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.chest.ChestUpgrades;
import com.comedymonkey.createaddons.content.chest.UpgradedBarrelBlock;
import com.comedymonkey.createaddons.content.chest.UpgradedChestBlock;
import com.comedymonkey.createaddons.content.chest.UpgradedChestBlockEntity;
import com.comedymonkey.createaddons.content.tradebot.TradeBotEntity;
import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipLibrary;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * One go at a container (server): the clip for its kind, the real lid opening and closing with it, and the items moving
 * on the clip's beat, never at its start (D5, D6). He takes the items {@code wanted} into his load until it's full,
 * returns unsold items {@code wanted} from his load, or delivers his earnings: emeralds and Nuggets of Experience, the
 * only things an output ever gets (D48, D57). If he's interrupted the lid is shut again.
 */
public class ContainerWork implements BotWork {
	/** How many of a stack he wants to move (0 for none), told what did move so the next stack can count it. */
	@FunctionalInterface
	public interface Wanted {
		int howMany(ItemStack stack);

		default void moved(ItemStack stack) {}

		/** All of every stack {@code which} accepts. */
		static Wanted all(Predicate<ItemStack> which) {
			return stack -> which.test(stack) ? stack.getCount() : 0;
		}
	}

	private final TradeBotEntity bot;
	private final ContainerVisit visit;
	public enum Mode { TAKE, RETURN, DELIVER_EARNINGS }

	private final Mode mode;
	private final boolean taking;
	private final Wanted wanted;
	private boolean opened, moved, closed;
	private int movedCount;
	/** Of what was delivered, the emeralds. */
	private int emeraldsDelivered;
	private final List<ItemStack> movedStacks = new ArrayList<>();

	public ContainerWork(TradeBotEntity bot, ContainerVisit visit, Mode mode, Wanted wanted) {
		this.bot = bot;
		this.visit = visit;
		this.mode = mode;
		this.taking = mode == Mode.TAKE;
		this.wanted = wanted;
	}

	public Mode mode() {
		return mode;
	}

	/** What moved, one stack per slot moved (counts included). */
	public List<ItemStack> movedStacks() {
		return movedStacks;
	}

	public ContainerVisit visit() {
		return visit;
	}

	@Override
	public BlockPos standBlock() {
		return visit.standBlock();
	}

	@Override
	public Vec3 stand() {
		return visit.stand();
	}

	@Override
	public float yaw() {
		return visit.yaw();
	}

	@Override
	public void start() {
		bot.playClip(ClipLibrary.get().clip(visit.kind().clip(taking)));
	}

	/** Runs the clip's beats; true once the clip is over. */
	@Override
	public boolean tick() {
		Clip clip = bot.clip();
		float t = bot.clipElapsed(0);
		ContainerKind kind = visit.kind();
		if (kind.opens && !opened && t >= kind.openAt) {
			setOpen(true);
			opened = true;
		}
		if (!moved && t >= kind.transferAt(taking)) {
			movedCount = switch (mode) {
				case TAKE -> take();
				case RETURN -> put();
				case DELIVER_EARNINGS -> deliverEarnings();
			};
			moved = true;
		}
		if (opened && !closed && t >= kind.closeAt(taking)) {
			setOpen(false);
			closed = true;
		}
		return !clip.name().equals(kind.clip(taking)) || clip.finished(t);
	}

	/** Interrupted (a home rule, damage, his station gone, packed, killed or unloaded): shut what he opened. */
	@Override
	public void abort() {
		if (opened && !closed) {
			setOpen(false);
			closed = true;
		}
	}

	public int emeraldsDelivered() {
		return emeraldsDelivered;
	}

	/** How many items moved. */
	public int moved() {
		return movedCount;
	}

	// ---- items

	@Nullable
	private IItemHandler handler() {
		if (!allHalvesLoaded(bot.level(), visit.container()))
			return null;
		return bot.level().getCapability(Capabilities.ItemHandler.BLOCK, visit.container(), visit.side());
	}

	private int take() {
		IItemHandler from = handler();
		if (from == null)
			return 0;
		return move(from, bot.getLoad(), wanted, movedStacks);
	}

	private int put() {
		IItemHandler to = handler();
		if (to == null)
			return 0;
		return move(bot.getLoad(), to, wanted, movedStacks);
	}

	/** Emeralds from the pouch, in stacks, then a nugget for every 3 XP banked; whatever doesn't fit stays with him. */
	private int deliverEarnings() {
		IItemHandler to = handler();
		if (to == null)
			return 0;
		int total = 0;
		while (bot.getEmeralds() > 0) {
			int count = (int) Math.min(64, bot.getEmeralds());
			ItemStack left = ItemHandlerHelper.insertItemStacked(to, new ItemStack(Items.EMERALD, count), false);
			int put = count - left.getCount();
			bot.addEarnings(-put, 0);
			total += put;
			emeraldsDelivered += put;
			if (put < count)
				break;
		}
		Item nugget = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "experience_nugget"));
		while (bot.getXpBank() >= TradeBotEntity.XP_PER_NUGGET) {
			int count = Math.min(64, bot.getXpBank() / TradeBotEntity.XP_PER_NUGGET);
			ItemStack left = ItemHandlerHelper.insertItemStacked(to, new ItemStack(nugget, count), false);
			int put = count - left.getCount();
			bot.addEarnings(0, -put * TradeBotEntity.XP_PER_NUGGET);
			total += put;
			if (put > 0)
				bot.awardOwner(com.comedymonkey.createaddons.advancement.ModAdvancements.TRADE_BOT_NUGGETS, put);
			if (put < count)
				break;
		}
		return total;
	}

	/** Moves what's wanted of each stack (as much as fits) from one handler to the other; returns the items moved. */
	public static int move(IItemHandler from, IItemHandler to, Wanted wanted, List<ItemStack> moved) {
		int total = 0;
		for (int slot = 0; slot < from.getSlots(); slot++) {
			ItemStack stack = from.getStackInSlot(slot);
			int want = stack.isEmpty() ? 0 : Math.min(stack.getCount(), wanted.howMany(stack));
			if (want <= 0)
				continue;
			ItemStack extracted = from.extractItem(slot, want, true);
			ItemStack left = ItemHandlerHelper.insertItemStacked(to, extracted, true);
			int count = extracted.getCount() - left.getCount();
			if (count <= 0)
				continue;
			ItemStack taken = from.extractItem(slot, count, false);
			ItemStack spill = ItemHandlerHelper.insertItemStacked(to, taken, false);
			if (!spill.isEmpty()) // shouldn't happen after the simulation; put it back rather than lose it
				from.insertItem(slot, spill, false);
			total += taken.getCount() - spill.getCount();
			moved.add(taken.copyWithCount(taken.getCount() - spill.getCount()));
			wanted.moved(moved.getLast());
		}
		return total;
	}

	// ---- the real block opening (D6: "barrels don't actually open or close" was the bug this fixes)

	private void setOpen(boolean open) {
		setOpen(bot.level(), visit.container(), open);
	}

	/** He has the lid open (or the barrel) right now. */
	public boolean isOpen() {
		return opened && !closed;
	}

	/**
	 * Shuts a container he left open (saved when he was unloaded mid-visit, closed once he's back); false if it isn't
	 * loaded, to try again later.
	 */
	public static boolean shut(Level level, BlockPos pos) {
		if (!level.isLoaded(pos))
			return false;
		setOpen(level, pos, false);
		return true;
	}

	/**
	 * Opens or shuts the real block, and the other half of a double chest. Only blocks that are loaded: he may be being
	 * unloaded with them, and nothing must be loaded back up for it.
	 */
	private static void setOpen(Level level, BlockPos pos, boolean open) {
		if (!level.isLoaded(pos))
			return;
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof ChestBlock) {
			level.blockEvent(pos, state.getBlock(), 1, open ? 1 : 0);
			if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
				BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
				if (level.isLoaded(other))
					level.blockEvent(other, level.getBlockState(other).getBlock(), 1, open ? 1 : 0);
			}
		} else if (state.getBlock() instanceof UpgradedChestBlock) {
			for (BlockPos half : loadedHalves(level, pos, state))
				if (level.getBlockEntity(half) instanceof UpgradedChestBlockEntity chest)
					chest.swingLid(open);
		} else if ((state.getBlock() instanceof BarrelBlock || state.getBlock() instanceof UpgradedBarrelBlock)
			&& state.hasProperty(BarrelBlock.OPEN) && state.getValue(BarrelBlock.OPEN) != open)
			level.setBlock(pos, state.setValue(BarrelBlock.OPEN, open), 3);
	}

	/** Both halves of a double chest, or just this one when the other half's chunk isn't loaded (it's not loaded for this). */
	public static List<BlockPos> loadedHalves(Level level, BlockPos pos, BlockState state) {
		if (state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
			&& state.hasProperty(ChestBlock.FACING) && !level.isLoaded(pos.relative(ChestBlock.getConnectedDirection(state))))
			return List.of(pos);
		return ChestUpgrades.halves(level, pos, state);
	}

	/**
	 * The container at {@code pos} and, for a double chest, its other half are both in loaded chunks: asking for a
	 * double chest's inventory reads both halves, so a half in an unloaded chunk would be loaded for it.
	 */
	public static boolean allHalvesLoaded(Level level, BlockPos pos) {
		if (!level.isLoaded(pos))
			return false;
		BlockState state = level.getBlockState(pos);
		return !state.hasProperty(ChestBlock.TYPE) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE
			|| !state.hasProperty(ChestBlock.FACING) || level.isLoaded(pos.relative(ChestBlock.getConnectedDirection(state)));
	}

	/** Load handler type, for callers that hold one. */
	public static boolean full(ItemStackHandler load) {
		for (int slot = 0; slot < load.getSlots(); slot++)
			if (load.getStackInSlot(slot).getCount() < load.getSlotLimit(slot))
				return false;
		return true;
	}
}
