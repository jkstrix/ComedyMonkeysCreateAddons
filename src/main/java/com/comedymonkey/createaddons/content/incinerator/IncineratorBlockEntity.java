package com.comedymonkey.createaddons.content.incinerator;

import java.util.List;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
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

/** Accepts every item and fluid offered and destroys it, keeping the fire lit for a few seconds after each. */
public class IncineratorBlockEntity extends BlockEntity implements IHaveGoggleInformation {
	/** How long the fire stays lit after the last thing burned, in ticks. */
	private static final int BURN_TICKS = 100;

	private final IItemHandler items = new ItemSink();
	private final IFluidHandler fluids = new FluidSink();
	private int burnTicks;
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
		if (burnTicks > 0 && --burnTicks == 0)
			setLit(false);
		// The goggles read the totals on the client: send them now and then while they change
		if (syncQueued && level.getGameTime() % 20 == 0) {
			syncQueued = false;
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
		}
	}

	private void burned() {
		if (burnTicks == 0)
			setLit(true);
		burnTicks = BURN_TICKS;
		syncQueued = true;
		setChanged();
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
		lang.translate(lit ? "gui.goggles.incinerator.burning" : "gui.goggles.incinerator.cold")
			.style(lit ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY)
			.forGoggles(tooltip, 1);
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.incinerator.items", itemsBurned)
			.style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.incinerator.fluid", fluidBurned)
			.style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		return true;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putInt("BurnTicks", burnTicks);
		tag.putLong("ItemsBurned", itemsBurned);
		tag.putLong("FluidBurned", fluidBurned);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		burnTicks = tag.getInt("BurnTicks");
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
				burned();
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
				burned();
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
