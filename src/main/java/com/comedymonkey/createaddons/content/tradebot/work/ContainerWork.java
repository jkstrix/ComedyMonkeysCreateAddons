package com.comedymonkey.createaddons.content.tradebot.work;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.tradebot.TradeBotEntity;
import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipLibrary;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * One go at a container (server): the clip for its kind, the real lid opening and closing with it, and the items moving
 * on the clip's beat, never at its start (D5, D6). Taking moves the items {@code wanted} into his load until it's full;
 * putting moves his load in until the container's full. If he's interrupted the lid is shut again.
 */
public class ContainerWork {
	private final TradeBotEntity bot;
	private final ContainerVisit visit;
	private final boolean taking;
	private final Predicate<ItemStack> wanted;
	private boolean opened, moved, closed;
	private int movedCount;

	public ContainerWork(TradeBotEntity bot, ContainerVisit visit, boolean taking, Predicate<ItemStack> wanted) {
		this.bot = bot;
		this.visit = visit;
		this.taking = taking;
		this.wanted = wanted;
	}

	public ContainerVisit visit() {
		return visit;
	}

	public void start() {
		bot.playClip(ClipLibrary.get().clip(visit.kind().clip(taking)));
	}

	/** Runs the clip's beats; true once the clip is over. */
	public boolean tick() {
		Clip clip = bot.clip();
		float t = bot.clipElapsed(0);
		ContainerKind kind = visit.kind();
		if (kind.opens && !opened && t >= kind.openAt) {
			setOpen(true);
			opened = true;
		}
		if (!moved && t >= kind.transferAt(taking)) {
			movedCount = taking ? take() : put();
			moved = true;
		}
		if (opened && !closed && t >= kind.closeAt(taking)) {
			setOpen(false);
			closed = true;
		}
		return !clip.name().equals(kind.clip(taking)) || clip.finished(t);
	}

	/** Interrupted (a home rule, damage, his station gone): shut what he opened. */
	public void abort() {
		if (opened && !closed) {
			setOpen(false);
			closed = true;
		}
	}

	/** How many items moved. */
	public int moved() {
		return movedCount;
	}

	// ---- items

	@Nullable
	private IItemHandler handler() {
		return bot.level().getCapability(Capabilities.ItemHandler.BLOCK, visit.container(), visit.side());
	}

	private int take() {
		IItemHandler from = handler();
		if (from == null)
			return 0;
		return move(from, bot.getLoad(), wanted);
	}

	private int put() {
		IItemHandler to = handler();
		if (to == null)
			return 0;
		return move(bot.getLoad(), to, wanted);
	}

	/** Moves whole stacks (or as much of each as fits) from one handler to the other; returns the items moved. */
	public static int move(IItemHandler from, IItemHandler to, Predicate<ItemStack> wanted) {
		int total = 0;
		for (int slot = 0; slot < from.getSlots(); slot++) {
			ItemStack stack = from.getStackInSlot(slot);
			if (stack.isEmpty() || !wanted.test(stack))
				continue;
			ItemStack extracted = from.extractItem(slot, stack.getCount(), true);
			ItemStack left = ItemHandlerHelper.insertItemStacked(to, extracted, true);
			int count = extracted.getCount() - left.getCount();
			if (count <= 0)
				continue;
			ItemStack taken = from.extractItem(slot, count, false);
			ItemStack spill = ItemHandlerHelper.insertItemStacked(to, taken, false);
			if (!spill.isEmpty()) // shouldn't happen after the simulation; put it back rather than lose it
				from.insertItem(slot, spill, false);
			total += taken.getCount() - spill.getCount();
		}
		return total;
	}

	// ---- the real block opening (D6: "barrels don't actually open or close" was the bug this fixes)

	private void setOpen(boolean open) {
		Level level = bot.level();
		BlockPos pos = visit.container();
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof ChestBlock) {
			level.blockEvent(pos, state.getBlock(), 1, open ? 1 : 0);
			if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
				BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
				level.blockEvent(other, level.getBlockState(other).getBlock(), 1, open ? 1 : 0);
			}
		} else if (state.getBlock() instanceof BarrelBlock && state.getValue(BarrelBlock.OPEN) != open)
			level.setBlock(pos, state.setValue(BarrelBlock.OPEN, open), 3);
	}

	/** Load handler type, for callers that hold one. */
	public static boolean full(ItemStackHandler load) {
		for (int slot = 0; slot < load.getSlots(); slot++)
			if (load.getStackInSlot(slot).getCount() < load.getSlotLimit(slot))
				return false;
		return true;
	}
}
