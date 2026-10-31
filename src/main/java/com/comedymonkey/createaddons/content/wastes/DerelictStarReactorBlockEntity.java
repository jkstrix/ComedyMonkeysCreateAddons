package com.comedymonkey.createaddons.content.wastes;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.electric.EnergyBuffer;
import com.comedymonkey.createaddons.content.line.PowerGrid;
import com.comedymonkey.createaddons.registry.ModWastes;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The heart's reactor (P10, P13), on its controller. Its city's {@link CityRecord} holds how long the heart's star burns
 * (as a game time, so the city's lamps go out on time even with the heart unloaded); this block follows it. While it
 * burns, it makes the Star Reactor's output into a store a Power Line can draw from (its ports), and runs on paper as a
 * {@link PowerGrid.Generator} while unloaded. Salvaged, it's a ruin: no star, no power, ever.
 */
public class DerelictStarReactorBlockEntity extends CityBlockEntity implements IHaveGoggleInformation, PowerGrid.Generator {
	private static final int BUFFER = 1_000_000;

	private final EnergyBuffer energy = new EnergyBuffer(BUFFER, 0, BUFFER, this::setChanged);
	private final IEnergyStorage energyOut = energy.extractOnly(() -> PowerGrid.givesHere(level, worldPosition));
	/** Game time its star burns until (the record's, when it has a city). */
	private long burnUntil;
	private long salvagedAt = SubstationBlockEntity.NEVER;
	private boolean burningShown;

	private final IItemHandler items = new IItemHandler() {
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
			if (stack.isEmpty() || !StarReactorBlockEntity.isStar(stack) || !canTakeStar())
				return stack;
			if (!simulate)
				insertStar(null);
			return stack.copyWithCount(stack.getCount() - 1);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			return 1;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return StarReactorBlockEntity.isStar(stack);
		}
	};

	public DerelictStarReactorBlockEntity(BlockPos pos, BlockState state) {
		super(ModWastes.DERELICT_STAR_REACTOR_BE.get(), pos, state);
	}

	public IItemHandler items() {
		return items;
	}

	public IEnergyStorage energyOut() {
		return energyOut;
	}

	public EnergyBuffer energy() {
		return energy;
	}

	public boolean isSalvaged() {
		return getBlockState().getValue(DerelictStarReactorBlock.SALVAGED);
	}

	public long salvagedAt() {
		return salvagedAt;
	}

	/** Its star is burning (on the client: as last synced). */
	public boolean isBurning() {
		if (level == null)
			return false;
		if (level.isClientSide)
			return burningShown;
		return !isSalvaged() && burnUntil > level.getGameTime();
	}

	public long burnLeft() {
		return level == null ? 0 : Math.max(0, burnUntil - level.getGameTime());
	}

	public boolean canTakeStar() {
		return !isSalvaged() && !isBurning();
	}

	/** A star in: all four districts wake at once, and the city runs on it for its burn (P10). */
	public boolean insertStar(@Nullable Player by) {
		if (!(level instanceof ServerLevel server) || !canTakeStar())
			return false;
		burnUntil = level.getGameTime() + CMConfig.SERVER.starBurnTicks();
		CityRecord record = record();
		if (record != null && link.bound()) {
			record.wakeAll(server, link.origin(), worldPosition);
			record.burnHeart(server, link.origin(), burnUntil, worldPosition);
		}
		if (by != null)
			ModAdvancements.award(by, ModAdvancements.WASTES_STAR_POWER);
		else
			ModAdvancements.awardNearby(level, worldPosition, ModAdvancements.WASTES_STAR_POWER, ItemStack.EMPTY, 0);
		show();
		setChanged();
		return true;
	}

	/** Taken apart with a wrench (P13): two Unknown Devices, its arms fall open, and it never runs again. */
	public boolean salvage(@Nullable Player player, ItemStack tool) {
		if (!(level instanceof ServerLevel server) || isSalvaged())
			return false;
		LootTable table = server.getServer().reloadableRegistries().getLootTable(Salvage.table(getBlockState().getBlock()));
		LootParams params = new LootParams.Builder(server)
			.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
			.withParameter(LootContextParams.TOOL, tool)
			.withParameter(LootContextParams.BLOCK_STATE, getBlockState())
			.withOptionalParameter(LootContextParams.THIS_ENTITY, player)
			.create(LootContextParamSets.BLOCK);
		for (ItemStack drop : table.getRandomItems(params))
			Block.popResource(level, worldPosition.above(), drop);
		level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, worldPosition.above(), Block.getId(getBlockState()));
		boolean wasBurning = isBurning();
		burnUntil = 0;
		salvagedAt = level.getGameTime();
		for (int p = 0; p < 27; p++) {
			BlockPos at = DerelictStarReactorBlock.partAt(worldPosition, p);
			BlockState there = level.getBlockState(at);
			if (there.getBlock() instanceof DerelictStarReactorBlock && !there.getValue(DerelictStarReactorBlock.SALVAGED))
				level.setBlock(at, there.setValue(DerelictStarReactorBlock.SALVAGED, true), Block.UPDATE_ALL);
		}
		CityRecord record = record();
		if (wasBurning && record != null && link.bound())
			record.burnHeart(server, link.origin(), 0, null);
		invalidateCapabilities();
		show();
		sync();
		return true;
	}

	/** Its blocks are going: a burning star goes out with them. */
	void removed() {
		if (isBurning() && link.bound() && level instanceof ServerLevel server)
			CityRecord.of(server).burnHeart(server, link.origin(), 0, null);
	}

	/** The record's burn is the heart's: it follows it (a star that burned out while it was unloaded, say). */
	@Override
	protected void recheck(boolean live, @Nullable BlockPos from) {
		CityRecord record = record();
		if (record == null || !link.bound() || isSalvaged())
			return;
		long until = record.heartUntil(link.origin());
		if (until != burnUntil) {
			burnUntil = until;
			setChanged();
		}
		show();
	}

	void tick() {
		if (!(level instanceof ServerLevel server))
			return;
		if (PowerGrid.away(level, worldPosition))
			return;
		PowerGrid.of(server).back(this, server);
		if (isBurning())
			energy.generate(CMConfig.SERVER.starReactorOutput());
		if (level.getGameTime() % 20 == 0)
			show();
		PowerGrid.of(server).note(this, server);
	}

	private void show() {
		boolean burning = isBurning();
		if (burning != burningShown) {
			burningShown = burning;
			sync();
		}
	}

	// ---------- its record, for running on paper ----------

	@Override
	public PowerGrid.Paper newPaper() {
		return new PowerGrid.Paper("derelict_star_reactor", 1);
	}

	@Override
	public void record(PowerGrid.Paper p, boolean refresh) {
		long left = isSalvaged() ? 0 : burnLeft();
		p.set(CMConfig.SERVER.starReactorOutput(), energy.getEnergyStored(), BUFFER);
		p.fuel(0).set(left, (int) Math.min(Integer.MAX_VALUE, Math.max(left, CMConfig.SERVER.starBurnTicks())), 1);
	}

	/** Back from paper: its store moves as its record's did (its burn is a time, so it's already right). */
	@Override
	public void resume(double storeChange, double[] fuelChange) {
		energy.setEnergy((int) Math.max(0, Math.min(BUFFER, energy.getEnergyStored() + Math.round(storeChange))));
		setChanged();
	}

	// ---------- saving ----------

	@Override
	protected void writeState(CompoundTag tag, HolderLookup.Provider registries) {
		tag.putLong("BurnUntil", burnUntil);
		tag.putInt("Energy", energy.getEnergyStored());
	}

	@Override
	protected void readState(CompoundTag tag, HolderLookup.Provider registries) {
		burnUntil = tag.getLong("BurnUntil");
		energy.setEnergy(tag.getInt("Energy"));
	}

	@Override
	protected void writeLooks(CompoundTag tag) {
		tag.putBoolean("Burning", burningShown);
		tag.putLong("SalvagedAt", salvagedAt);
	}

	@Override
	protected void readLooks(CompoundTag tag) {
		burningShown = tag.getBoolean("Burning");
		salvagedAt = tag.contains("SalvagedAt") ? tag.getLong("SalvagedAt") : SubstationBlockEntity.NEVER;
	}

	// ---------- goggles ----------

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.derelict_star_reactor").forGoggles(tooltip);
		if (isSalvaged()) {
			new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.derelict_star_reactor.salvaged").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
			return true;
		}
		if (!isBurning()) {
			new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.star_reactor.no_star").style(ChatFormatting.RED).forGoggles(tooltip, 1);
			return true;
		}
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.star_reactor.running",
				String.format(Locale.ROOT, "%,d", CMConfig.SERVER.starReactorOutput()))
			.style(ChatFormatting.GREEN).forGoggles(tooltip, 1);
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.star_reactor.left", StarReactorBlockEntity.duration(burnLeft()))
			.style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
		return true;
	}
}
