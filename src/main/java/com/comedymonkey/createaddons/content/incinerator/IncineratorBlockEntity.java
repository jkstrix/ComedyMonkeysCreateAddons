package com.comedymonkey.createaddons.content.incinerator;

import java.util.List;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.SaveMark;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Accepts every item and fluid offered and destroys it, water included. The fire ramps up and down: each item (or each
 * tenth of a bucket) adds a point of heat, and it loses a point a second. It lights, and heats a boiler above it, at
 * full heat (ten things), and stays lit until it has cooled right down, so it never flickers: keep feeding it at least
 * a thing a second and it stays hot; stop, and it's cold within ten seconds.
 */
public class IncineratorBlockEntity extends BlockEntity implements IHaveGoggleInformation {
	/** Full heat, in things burned: it lights here. */
	public static final int FULL_HEAT = 10;
	/** Millibuckets of fluid that count as one thing. */
	private static final int MB_PER_THING = 100;
	/** Ticks between losing a point of heat. */
	private static final int COOL_TICKS = 20;

	private final IItemHandler items = new ItemSink();
	private final IFluidHandler fluids = new FluidSink();
	private int heat;
	private int coolIn = COOL_TICKS;
	/** Millibuckets burned toward the next thing. */
	private int fluidToward;
	private long itemsBurned;
	private long fluidBurned;
	private boolean syncQueued;

	public IncineratorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.INCINERATOR.get(), pos, state);
	}

	public IItemHandler getItemHandler() {
		return items;
	}

	public IFluidHandler getFluidHandler() {
		return fluids;
	}

	void serverTick() {
		if (heat > 0 && --coolIn <= 0) {
			heat--;
			coolIn = COOL_TICKS;
			syncQueued = true;
			setChanged();
		}
		// lit at full heat, out once it's cooled right down: nothing in between switches it
		if (heat >= FULL_HEAT)
			setLit(true);
		else if (heat == 0)
			setLit(false);
		// The goggles read the heat and totals on the client: send them now and then while they change
		if (syncQueued && level != null && level.getGameTime() % 20 == 0) {
			syncQueued = false;
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
		}
	}

	private void burned(long things) {
		if (things <= 0)
			return;
		if (heat == 0)
			coolIn = COOL_TICKS; // a cold fire starts its first second fresh
		heat = (int) Math.min(FULL_HEAT, heat + things);
		syncQueued = true;
		// fed by pipes many times a tick, and no comparator output: just a save
		SaveMark.mark(this);
	}

	private void setLit(boolean lit) {
		if (level == null || level.isClientSide)
			return;
		BlockState state = getBlockState();
		if (state.getValue(IncineratorBlock.LIT) != lit)
			level.setBlock(worldPosition, state.setValue(IncineratorBlock.LIT, lit), 3);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		LangBuilder lang = new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID);
		lang.translate("gui.goggles.incinerator").forGoggles(tooltip);
		boolean lit = getBlockState().getValue(IncineratorBlock.LIT);
		if (lit)
			lang.translate("gui.goggles.incinerator.burning").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
		else if (heat > 0)
			lang.translate("gui.goggles.incinerator.warming", heat, FULL_HEAT).style(ChatFormatting.YELLOW).forGoggles(tooltip, 1);
		else
			lang.translate("gui.goggles.incinerator.cold").style(ChatFormatting.DARK_GRAY).forGoggles(tooltip, 1);
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.incinerator.items", itemsBurned)
			.style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.incinerator.fluid", fluidBurned)
			.style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		return true;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putInt("Heat", heat);
		tag.putInt("CoolIn", coolIn);
		tag.putInt("FluidToward", fluidToward);
		tag.putLong("ItemsBurned", itemsBurned);
		tag.putLong("FluidBurned", fluidBurned);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		heat = tag.getInt("Heat");
		coolIn = tag.contains("CoolIn") ? tag.getInt("CoolIn") : COOL_TICKS;
		fluidToward = tag.getInt("FluidToward");
		itemsBurned = tag.getLong("ItemsBurned");
		fluidBurned = tag.getLong("FluidBurned");
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** One bottomless slot: everything goes in, nothing comes out. */
	private class ItemSink implements IItemHandler {
		@Override
		public int getSlots() {
			return 1;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return ItemStack.EMPTY;
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (stack.isEmpty())
				return stack;
			if (!simulate) {
				itemsBurned += stack.getCount();
				burned(stack.getCount());
			}
			return ItemStack.EMPTY;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			return 64;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return true;
		}
	}

	/** Takes any fluid, any amount. */
	private class FluidSink implements IFluidHandler {
		@Override
		public int getTanks() {
			return 1;
		}

		@Override
		public FluidStack getFluidInTank(int tank) {
			return FluidStack.EMPTY;
		}

		@Override
		public int getTankCapacity(int tank) {
			return Integer.MAX_VALUE;
		}

		@Override
		public boolean isFluidValid(int tank, FluidStack stack) {
			return true;
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			if (resource.isEmpty())
				return 0;
			if (action.execute()) {
				fluidBurned += resource.getAmount();
				long toward = (long) fluidToward + resource.getAmount();
				fluidToward = (int) (toward % MB_PER_THING);
				burned(toward / MB_PER_THING);
			}
			return resource.getAmount();
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			return FluidStack.EMPTY;
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return FluidStack.EMPTY;
		}
	}
}
