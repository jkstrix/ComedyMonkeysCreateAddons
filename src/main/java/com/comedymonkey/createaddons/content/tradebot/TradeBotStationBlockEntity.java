package com.comedymonkey.createaddons.content.tradebot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.electric.EnergyBuffer;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.comedymonkey.createaddons.registry.ModDataComponents;
import com.comedymonkey.createaddons.registry.ModItems;
import com.comedymonkey.createaddons.registry.ModSounds;
import com.comedymonkey.createaddons.network.TradeBotStationPayload;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Trade Bot's station: FE comes in from underneath (D16) and goes into him while he's docked (D21). It knows its
 * owner, its bot, and the containers it's linked to (D19, D22); a redstone signal keeps him home (D38).
 */
public class TradeBotStationBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
	public static final int MAX_INPUTS = 4;
	/** Comparator strengths (D58). */
	public static final int SIGNAL_NONE = 0, SIGNAL_DOCKED = 5, SIGNAL_WORKING = 10, SIGNAL_OUT_OF_CHARGE = 15;
	/** Below this share of a full charge he counts as out of charge. */
	private static final float LOW_CHARGE = 0.05f;

	private final EnergyBuffer energy;
	@Nullable
	private UUID owner;
	@Nullable
	private UUID bot;
	/** Linked containers, relative to the station. */
	private final List<BlockPos> inputs = new ArrayList<>();
	@Nullable
	private BlockPos output;
	private boolean paused;
	/** Set while packing up, so removing the block doesn't take the bot down with it. */
	private boolean packing;

	// what the server last saw of the bot, for goggles, comparators and the renderer (he may be unloaded)
	private int botCharge;
	private boolean botDocked;
	private boolean botCharging;
	private boolean botWoundDown;
	private int botEntityId = -1;
	private int lastSignal = -1;
	private int syncedEnergy = -1;

	public TradeBotStationBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.TRADE_BOT_STATION.get(), pos, state);
		int buffer = CMConfig.SERVER.tradeBotStationBuffer();
		energy = new EnergyBuffer(buffer, buffer, 0, this::energyChanged);
	}

	/**
	 * Its stored FE changed (every tick while he charges): saved with the chunk, but no neighbour updates, as the
	 * comparator signal doesn't depend on it ({@link #tick} updates comparators itself when the signal changes).
	 */
	private void energyChanged() {
		if (level != null)
			level.blockEntityChanged(worldPosition);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	/** Takes FE from underneath only (and from no side, for things that ask without one). */
	@Nullable
	public EnergyBuffer getEnergy(@Nullable Direction side) {
		return side == null || side == Direction.DOWN ? energy : null;
	}

	public EnergyBuffer getEnergy() {
		return energy;
	}

	// ---- setup, from the item

	void claim(UUID owner, UUID bot, TradeBotData data) {
		this.owner = owner;
		this.bot = bot;
		inputs.clear();
		data.inputs().stream().limit(MAX_INPUTS).forEach(inputs::add);
		output = data.output().orElse(null);
		notifyUpdate();
	}

	@Nullable
	public UUID getOwner() {
		return owner;
	}

	@Nullable
	public UUID getBotId() {
		return bot;
	}

	public Direction facing() {
		return getBlockState().getValue(TradeBotStationBlock.FACING);
	}

	/** His bot, if it's loaded on this server. */
	@Nullable
	public TradeBotEntity findBot() {
		if (bot == null || !(level instanceof ServerLevel server))
			return null;
		Entity entity = server.getEntity(bot);
		return entity instanceof TradeBotEntity tradeBot ? tradeBot : null;
	}

	/** Client: the bot this station belongs to, if it's in view. */
	@Nullable
	public TradeBotEntity clientBot() {
		return level != null && botEntityId >= 0 && level.getEntity(botEntityId) instanceof TradeBotEntity tradeBot ? tradeBot : null;
	}

	public boolean isBotDocked() {
		return botDocked;
	}

	/** His charge as last seen (FE). */
	public int botCharge() {
		return botCharge;
	}

	/** Docked and taking charge right now. */
	public boolean isCharging() {
		return botCharging;
	}

	// ---- links (D22, D61), kept relative to the station

	/** The containers he takes from, in the world. */
	public List<BlockPos> inputPositions() {
		return inputs.stream().map(worldPosition::offset).toList();
	}

	/** The container he delivers to, in the world, or null. */
	@Nullable
	public BlockPos outputPosition() {
		return output == null ? null : worldPosition.offset(output);
	}

	public TradeBotLinks.Role roleOf(BlockPos target) {
		BlockPos relative = target.subtract(worldPosition);
		if (inputs.contains(relative))
			return TradeBotLinks.Role.TAKES;
		return relative.equals(output) ? TradeBotLinks.Role.DELIVERS : TradeBotLinks.Role.NONE;
	}

	/** One more click on {@code target} while picking: takes from, then delivers to, then unlinked. Returns the new role. */
	TradeBotLinks.Role cycleLink(BlockPos target) {
		boolean beltOnly = level != null && TradeBotLinks.outputOnly(level, target);
		TradeBotLinks.Role next = TradeBotLinks.nextRole(roleOf(target), beltOnly, inputs.size(), MAX_INPUTS, output != null);
		setRole(target, next);
		return next;
	}

	/** A player's action from the station screen or while picking containers (D61). Owner (and team) only. */
	public void handle(ServerPlayer player, TradeBotStationPayload.Action action, BlockPos target) {
		if (level == null)
			return;
		if (!TradeBotOwnership.canControl(player, owner)) {
			player.displayClientMessage(Component.translatable("message.comedymonkeysaddons.trade_bot.not_yours"), true);
			return;
		}
		switch (action) {
			case CALL_HOME -> {
				if (player.distanceToSqr(worldPosition.getCenter()) > 64)
					return;
				TradeBotEntity tradeBot = findBot();
				if (tradeBot != null && !tradeBot.isDocked())
					player.displayClientMessage(Component.translatable(tradeBot.goHome() ? "message.comedymonkeysaddons.trade_bot.heading_home"
						: "message.comedymonkeysaddons.trade_bot.wind_up_first"), true);
			}
			case UNLINK -> {
				// from the screen (by the station) or left-clicked while picking (by the container)
				boolean near = player.distanceToSqr(worldPosition.getCenter()) <= 64 || player.distanceToSqr(target.getCenter()) <= 64;
				if (!near)
					return;
				// a linked block unlinks straight off, even out in an unloaded chunk; only a loaded one is looked at, for
				// the other half of a linked double chest and for its name
				boolean loaded = level.isLoaded(target);
				BlockPos linked = roleOf(target) != TradeBotLinks.Role.NONE || !loaded ? target : linkedHalf(target);
				if (roleOf(linked) == TradeBotLinks.Role.NONE)
					return;
				setRole(linked, TradeBotLinks.Role.NONE);
				if (level.isLoaded(linked))
					player.displayClientMessage(TradeBotLinks.describe(level, linked, TradeBotLinks.Role.NONE), true);
			}
			case START_PICKING -> {
				if (player.distanceToSqr(worldPosition.getCenter()) <= 64)
					TradeBotPicking.start(player, worldPosition);
			}
			case STOP_PICKING -> TradeBotPicking.stop(player);
		}
	}

	/**
	 * The block a click on {@code clicked} means: a double chest is one container, so clicking the other half of a linked
	 * one acts on the half that's linked.
	 */
	public BlockPos linkedHalf(BlockPos clicked) {
		if (level == null || roleOf(clicked) != TradeBotLinks.Role.NONE)
			return clicked;
		if (!level.isLoaded(clicked))
			return clicked;
		for (BlockPos half : com.comedymonkey.createaddons.content.tradebot.work.ContainerWork.loadedHalves(level, clicked, level.getBlockState(clicked)))
			if (roleOf(half) != TradeBotLinks.Role.NONE)
				return half;
		return clicked;
	}

	/** A container clicked while picking (from {@link TradeBotPicking}): cycle its link, if it can be linked. */
	void pick(ServerPlayer player, BlockPos clicked) {
		if (level == null || !TradeBotOwnership.canControl(player, owner))
			return;
		BlockPos target = linkedHalf(clicked);
		if (roleOf(target) == TradeBotLinks.Role.NONE && !TradeBotLinks.canLink(level, target)) {
			player.displayClientMessage(Component.translatable("message.comedymonkeysaddons.trade_bot.link.not_a_container"), true);
			return;
		}
		if (!TradeBotLinks.inRange(worldPosition, target)) {
			player.displayClientMessage(Component.translatable("message.comedymonkeysaddons.trade_bot.link.too_far", CMConfig.SERVER.tradeBotRange()), true);
			return;
		}
		TradeBotLinks.Role before = roleOf(target);
		TradeBotLinks.Role after = cycleLink(target);
		if (before == TradeBotLinks.Role.NONE && after == TradeBotLinks.Role.NONE)
			player.displayClientMessage(Component.translatable("message.comedymonkeysaddons.trade_bot.link.full"), true);
		else
			player.displayClientMessage(TradeBotLinks.describe(level, target, after), true);
	}

	/** Links (or unlinks) a container directly, without checks (the picking and the game tests use it). */
	public void setRole(BlockPos target, TradeBotLinks.Role role) {
		BlockPos relative = target.subtract(worldPosition);
		inputs.remove(relative);
		if (relative.equals(output))
			output = null;
		switch (role) {
			case TAKES -> inputs.add(relative);
			case DELIVERS -> output = relative; // a new output replaces the old one
			case NONE -> {}
		}
		notifyUpdate();
	}

	// ---- ticking: charge him, keep the glow, the comparator and clients up to date

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;
		TradeBotEntity tradeBot = findBot();
		boolean docked = tradeBot != null && tradeBot.isDocked();
		if (docked) {
			int give = Math.min(energy.getEnergyStored(), Math.min(CMConfig.SERVER.tradeBotChargeFePerTick(), tradeBot.chargeRoom()));
			if (give > 0) {
				energy.consume(give);
				tradeBot.addCharge(give);
			}
			tradeBot.setCharging(give > 0);
		}
		boolean charging = docked && tradeBot.isCharging();
		boolean changed = false;
		if (tradeBot != null) {
			// his charge only as often as what it shows changes: the goggles' whole percent, and the comparator's low mark
			int charge = tradeBot.getCharge();
			changed |= level.getGameTime() % 10 == 0 && (shownPercent(botCharge) != shownPercent(charge) || lowCharge(botCharge) != lowCharge(charge));
			changed |= botEntityId != tradeBot.getId() || botDocked != docked || botCharging != charging || botWoundDown != tradeBot.isWoundDown();
			if (changed) {
				botCharge = tradeBot.getCharge();
				botEntityId = tradeBot.getId();
				botDocked = docked;
				botCharging = charging;
				botWoundDown = tradeBot.isWoundDown();
			}
		}
		BlockState state = getBlockState();
		if (state.getValue(TradeBotStationBlock.GLOW) != botDocked)
			level.setBlock(worldPosition, state.setValue(TradeBotStationBlock.GLOW, botDocked), 3);
		int signal = comparatorOutput();
		if (signal != lastSignal) {
			lastSignal = signal;
			level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
		}
		if (level.getGameTime() % 10 == 0 && Math.abs(energy.getEnergyStored() - syncedEnergy) > energy.getMaxEnergyStored() / 100) {
			syncedEnergy = energy.getEnergyStored();
			changed = true;
		}
		if (changed)
			sendData();
	}

	/** His charge as the goggles show it, a whole percent of a full charge. */
	static int shownPercent(int charge) {
		return Math.round(100f * charge / CMConfig.SERVER.tradeBotFullCharge());
	}

	private static boolean lowCharge(int charge) {
		return charge < CMConfig.SERVER.tradeBotFullCharge() * LOW_CHARGE;
	}

	public int comparatorOutput() {
		if (bot == null)
			return SIGNAL_NONE;
		// out of charge, or run down (he needs his owner either way)
		if (botWoundDown || botDocked && lowCharge(botCharge))
			return SIGNAL_OUT_OF_CHARGE;
		return botDocked ? SIGNAL_DOCKED : SIGNAL_WORKING;
	}

	public boolean isPaused() {
		return paused;
	}

	void setPaused(boolean paused) {
		if (this.paused == paused)
			return;
		this.paused = paused;
		TradeBotEntity tradeBot = findBot();
		if (paused && tradeBot != null && !tradeBot.isDocked())
			tradeBot.goHome();
		notifyUpdate();
	}

	// ---- packing up (D27, D30)

	/** The owner's sneak-wrench: packs station and bot into one item, once he's home. */
	void pack(ServerPlayer player) {
		if (level == null)
			return;
		if (owner != null && !TradeBotOwnership.canControl(player, owner)) {
			player.displayClientMessage(Component.translatable("message.comedymonkeysaddons.trade_bot.not_yours"), true);
			return;
		}
		TradeBotEntity tradeBot = findBot();
		boolean gone = bot == null || TradeBotRegistry.get(player.server).isGone(bot);
		if (tradeBot == null && !gone) {
			player.displayClientMessage(Component.translatable("message.comedymonkeysaddons.trade_bot.out_of_reach"), true);
			return;
		}
		if (tradeBot != null && !tradeBot.isDocked()) {
			// run down out there, he has to be wound up before he can come home
			player.displayClientMessage(Component.translatable(tradeBot.goHome() ? "message.comedymonkeysaddons.trade_bot.heading_home"
				: "message.comedymonkeysaddons.trade_bot.wind_up_first"), true);
			return;
		}
		TradeBotData data = tradeBot != null ? tradeBot.toData(inputs, Optional.ofNullable(output)) : TradeBotData.EMPTY;
		ItemStack stack = new ItemStack(ModItems.TRADE_BOT.get());
		stack.set(ModDataComponents.TRADE_BOT_DATA.get(), data);
		if (tradeBot != null && tradeBot.hasCustomName())
			stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, tradeBot.getCustomName());
		packing = true;
		if (bot != null)
			TradeBotRegistry.get(player.server).remove(bot);
		if (tradeBot != null)
			tradeBot.packUp();
		level.playSound(null, worldPosition, ModSounds.tradeBot("winder_release"), SoundSource.BLOCKS, 1, 1);
		level.removeBlock(worldPosition, false);
		if (!player.getInventory().add(stack))
			player.drop(stack, false);
	}

	/**
	 * The block went some other way than packing: his bot goes too, dropping what he carries (packing keeps it in the
	 * item instead), and stops counting toward the limit.
	 */
	void removed() {
		if (packing || bot == null || !(level instanceof ServerLevel server))
			return;
		TradeBotEntity tradeBot = findBot();
		if (tradeBot != null) {
			tradeBot.dropCarried();
			tradeBot.packUp();
		}
		TradeBotRegistry.get(server.getServer()).remove(bot);
	}

	// ---- trades today (D39)

	private int tradesToday;
	private long tradesDay = -1;

	/** His bot made a trade: counted toward today's, which start again each morning. */
	public void countTrade() {
		long day = level.getDayTime() / 24000;
		if (day != tradesDay) {
			tradesDay = day;
			tradesToday = 0;
		}
		tradesToday++;
		sendData();
	}

	public int tradesToday() {
		return level != null && level.getDayTime() / 24000 == tradesDay ? tradesToday : 0;
	}

	// ---- goggles (D39)

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.trade_bot").forGoggles(tooltip);
		int full = CMConfig.SERVER.tradeBotFullCharge();
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID)
			.translate("gui.goggles.trade_bot.charge", String.format(Locale.ROOT, "%.0f%%", 100f * botCharge / full))
			.style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID)
			.translate("gui.goggles.trade_bot.station", String.format(Locale.ROOT, "%,d / %,d FE", energy.getEnergyStored(), energy.getMaxEnergyStored()))
			.style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		String status = bot == null ? "no_bot" : botWoundDown ? "wound_down" : paused ? "paused" : botDocked ? "docked" : "working";
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.trade_bot.status." + status)
			.style(botWoundDown ? ChatFormatting.RED : botDocked || bot == null ? ChatFormatting.GOLD : ChatFormatting.GREEN).forGoggles(tooltip, 1);
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID)
			.translate("gui.goggles.trade_bot.links", inputs.size(), MAX_INPUTS,
				Component.translatable(output != null ? "comedymonkeysaddons.gui.goggles.trade_bot.output_set"
					: "comedymonkeysaddons.gui.goggles.trade_bot.output_unset"))
			.style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.trade_bot.trades_today", tradesToday())
			.style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		return true;
	}

	// ---- saving

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.putInt("Energy", energy.getEnergyStored());
		if (owner != null)
			tag.putUUID("Owner", owner);
		if (bot != null)
			tag.putUUID("Bot", bot);
		ListTag list = new ListTag();
		for (BlockPos input : inputs)
			list.add(NbtUtils.writeBlockPos(input));
		tag.put("Inputs", list);
		if (output != null)
			tag.put("Output", NbtUtils.writeBlockPos(output));
		tag.putBoolean("Paused", paused);
		tag.putInt("BotCharge", botCharge);
		tag.putBoolean("BotDocked", botDocked);
		tag.putBoolean("BotCharging", botCharging);
		tag.putBoolean("BotWoundDown", botWoundDown);
		tag.putInt("TradesToday", tradesToday);
		tag.putLong("TradesDay", tradesDay);
		if (clientPacket)
			tag.putInt("BotEntity", botEntityId);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		energy.setEnergy(tag.getInt("Energy"));
		owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
		bot = tag.hasUUID("Bot") ? tag.getUUID("Bot") : null;
		inputs.clear();
		for (Tag t : tag.getList("Inputs", Tag.TAG_INT_ARRAY))
			inputs.add(NbtUtils.readBlockPos(wrap(t), "Pos").orElse(BlockPos.ZERO));
		output = NbtUtils.readBlockPos(tag, "Output").orElse(null);
		paused = tag.getBoolean("Paused");
		botCharge = tag.getInt("BotCharge");
		botDocked = tag.getBoolean("BotDocked");
		botCharging = tag.getBoolean("BotCharging");
		botWoundDown = tag.getBoolean("BotWoundDown");
		tradesToday = tag.getInt("TradesToday");
		tradesDay = tag.contains("TradesDay") ? tag.getLong("TradesDay") : -1;
		if (clientPacket)
			botEntityId = tag.getInt("BotEntity");
	}

	private static CompoundTag wrap(Tag pos) {
		CompoundTag tag = new CompoundTag();
		tag.put("Pos", pos);
		return tag;
	}
}
