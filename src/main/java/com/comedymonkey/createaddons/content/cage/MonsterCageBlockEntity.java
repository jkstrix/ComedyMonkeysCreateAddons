package com.comedymonkey.createaddons.content.cage;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.ClientView;
import com.comedymonkey.createaddons.content.Drops;
import com.comedymonkey.createaddons.content.FillOnly;
import com.comedymonkey.createaddons.content.FixedStress;
import com.comedymonkey.createaddons.content.MachineEnchantments;
import com.comedymonkey.createaddons.content.SettlingImpact;
import com.comedymonkey.createaddons.content.SyncStagger;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.comedymonkey.createaddons.registry.ModItems;
import com.comedymonkey.createaddons.registry.ModSounds;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.world.level.material.Fluids;

/**
 * A Monster Cage (design/specs/monster-cage-and-fish-tank.md, Part 1). It keeps one jarred monster and a horde of the
 * same kind: while the shaft turns, a new one arrives every interval (sooner the faster it turns, and with Monstrous),
 * each eating its Monster Chow (and, in a Flooded cage, 250 mB of water). A sword or a Deployer with one culls half,
 * for the monster's real loot. Copies arrive bare: they never wear the jarred one's gear (D27), but a drowned now and
 * then comes armed with a trident (D60) and throws it out of the front at players (D61). Zombies and husks put in a
 * Flooded cage drown into drowned (D56).
 * <p>
 * The Netherite cage holds Withers on Forge Energy (D20, D42): the field takes a flat amount whenever a Wither is in,
 * farming more by speed. With the field down a 5-minute countdown runs, announced to everyone within range, while
 * someone is near (D74-D77, D44, D76); then one Wither breaks out, and another every game day until it's empty.
 */
public class MonsterCageBlockEntity extends KineticBlockEntity implements Clearable, MenuProvider {
	/** Monsters the window shows at most (one Wither). */
	public static final int SHOWN = 4;
	private static final int SYNC_INTERVAL = 10;
	/** A new drowned comes armed this often, and an armed one culled drops its trident this often (+1% per Looting). */
	private static final float ARMED_CHANCE = 0.0625f, TRIDENT_DROP = 0.085f;
	/** Ticks a zombie takes to drown into a drowned (and a husk into a zombie), as in vanilla. */
	private static final int DROWN_TICKS = 600;
	/** Trident throws: range, and the ticks between them. */
	private static final double THROW_RANGE = 16;
	private static final int THROW_COOLDOWN = 60, HATCH_SHUT_TICKS = 17;
	/** Ticks between looks for a target while an armed drowned has none. */
	private static final int SEARCH_COOLDOWN = 10;
	/** The klaxon's blasts in a burst, this many ticks apart; and continuous in the last 10 s. */
	private static final int BLAST_GAP = 32;

	/** Client: run each tick for its sounds (set by the client setup; nothing on a dedicated server). */
	public static Consumer<MonsterCageBlockEntity> clientTicker = be -> {};

	private ItemStack captive = ItemStack.EMPTY;
	private ItemEnchantments enchantments = ItemEnchantments.EMPTY;
	/** Monsters in the cage, the jarred one included. */
	private int count;
	/** Drowned holding tridents. */
	private int armed;
	/** Towards the next arrival, in ticks at the reference speed. */
	private float progress;
	private final ChowHandler chow;
	private final ItemStackHandler output;
	private final FluidTank water;
	private final IFluidHandler waterIn;
	private final CageEnergy energy;
	private final IItemHandler automation = new AutomationHandler();
	private final IItemHandlerModifiable captiveSlot = new CaptiveHandler();
	/** The captive's monster, read once whenever the captive changes (it's asked several times a tick). */
	@Nullable
	private EntityType<?> monsterType;
	private boolean syncQueued;
	/** Queued syncs go out on this tick of every {@link #SYNC_INTERVAL}, by position, so a wall of cages doesn't sync all at once. */
	private final int syncPhase;
	/** Server: the captive as last sent to clients, when, and this cage's own last sync (as the tanks'). */
	@Nullable
	private ItemStack sentCaptive;
	private long sentCaptiveAt, ownSyncAt;
	private boolean omitCaptive;
	private static final int CAPTIVE_RESEND_TICKS = 100;
	/** Client: the captive as the server last sent it (a click here may have guessed another the server refused). */
	@Nullable
	private ItemStack receivedCaptive;
	/** Client: the stress impact the server's network holds for it (NaN until sent), for the goggles. */
	private float syncedImpact = Float.NaN;
	/** Client: the bowl and the tray as the server summed them up (clients aren't sent the inventories; an open menu syncs its own slots). */
	private ItemStack shownChow = ItemStack.EMPTY;
	private int shownChowCount, trayUsed;
	private final List<ItemStack> tray = new ArrayList<>(TRAY_SHOWN);
	private static final int TRAY_SHOWN = 3;
	/** The stress impact the network holds: a rise at once, a fall once it has lasted (see {@link SettlingImpact}). */
	private final SettlingImpact impact = new SettlingImpact();
	@Nullable
	private Entity sample;
	@Nullable
	private AABB renderBox;

	// the Netherite cage's field
	private boolean fieldUp;
	/** Whether its farming draw is powered this tick (shed first when short). */
	private boolean farmingPowered = true;
	/** Ticks until the next Wither breaks out, while the field is down; -1 when nothing is counting. */
	private int escapeIn = -1;
	/** The countdown is stopped: nobody is near (D76). */
	private boolean paused;
	@Nullable
	private UUID owner;
	private String ownerName = "";
	/**
	 * The FE changes every tick, so it doesn't sync the cage: the open screen reads it through the menu, and clients'
	 * goggles get it with the next sync, or at most every {@link #ENERGY_SYNC_TICKS} while it's changing. Sent: the FE at
	 * the last sync, and when.
	 */
	private boolean energyChanged;
	private int sentEnergy;
	private long sentEnergyAt;
	private static final int ENERGY_SYNC_TICKS = 20;
	/** Whether comparators were last told of a countdown (they read 15 through one). */
	private boolean toldWarning;
	/**
	 * The field off takes no FE at all (user, 2026-10-27): it notes what's offered instead, each call's amount, simulated
	 * or not, this tick and for each of the last {@link #SUPPLY_TICKS}. Only a second's offers covering the field's cost
	 * start it.
	 */
	private long offeredNow, offeredSum;
	private final long[] offered = new long[SUPPLY_TICKS];
	private int offeredAt, offeredTicks;
	private static final int SUPPLY_TICKS = 20;
	/**
	 * A trial: a second's offers covered the cost, so the field takes FE again (at most its cost a tick, kept in its
	 * store, not drawn), and it comes up once that much has come in every tick for {@link #SUPPLY_TICKS} ticks. Some
	 * suppliers offer more than they give (a Power Line offers its whole capacity; a generator, its whole store): their
	 * FE has to arrive. The first may wait for a Power Line's next round to send any. -1 while not on trial.
	 */
	private int startingFor = -1;
	private int startingFull;
	private long receivedNow;
	/** Ticks a trial may wait for the first FE to come in (a Power Line plans once a second). */
	private static final int START_WAIT = 2 * SUPPLY_TICKS;
	/**
	 * After a failed trial, offers can't start another until {@link #retryAt}: 10 s, doubling with each failure to a
	 * minute, and back to none once a field has held for the hold (user, 2026-10-27: short trial, slow retries).
	 */
	private long retryAt;
	private int retryWait;
	private static final int RETRY_FIRST = 200, RETRY_MOST = 1200;
	/**
	 * Ticks the field has held since it came up. A countdown stands still while it holds, and is called off only once
	 * it has held for the config's hold (a minute); a field that drops before then lets the countdown carry on where it
	 * stood (user, 2026-10-27: bursts pause the countdown, they don't cancel it).
	 */
	private int heldFor;

	// the Flooded cages
	private int drownTicks;
	private int throwCooldown;
	private int hatchShutIn;
	/** Client: when the hatch last flapped open, for the renderer. */
	private long hatchOpenedAt = Long.MIN_VALUE;

	private int grumbleIn = 200;
	private long lastPour = Long.MIN_VALUE, lastDrop = Long.MIN_VALUE;
	/** Client only: the monsters walking about in the window. */
	private final List<CageMonster> shown = new ArrayList<>();
	/** Client: the jar's monster couldn't be made (its mod gone, a tag changed): not tried again until the jar changes. */
	private boolean createFailed;
	/** Client: the camera's distance this tick (for the window and the loops); endless where it can't be told. */
	private double viewDistanceSqr;
	private final BooleanSupplier mayBuild = () -> ClientView.mayBuild(level);

	public MonsterCageBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.MONSTER_CAGE.get(), pos, state);
		CageTier tier = tierOf(state);
		chow = new ChowHandler(tier.chowSlots());
		output = new ItemStackHandler(tier.outputSlots()) {
			@Override
			protected void onContentsChanged(int slot) {
				onInventoryChanged();
			}
		};
		int tank = !isFlooded(state) ? 0 : CMConfig.Server.get(tier == CageTier.BRASS ? CMConfig.SERVER.brassFloodedCageTank
			: CMConfig.SERVER.floodedCageTank);
		water = new FluidTank(Math.max(1, tank), stack -> stack.getFluid().isSame(Fluids.WATER)) {
			@Override
			protected void onContentsChanged() {
				onInventoryChanged();
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return isFlooded() ? super.fill(resource, action) : 0;
			}
		};
		waterIn = new FillOnly(water);
		energy = new CageEnergy(tier == CageTier.NETHERITE ? CMConfig.Server.get(CMConfig.SERVER.cageFeBuffer) : 0);
		syncPhase = SyncStagger.phase(pos, SYNC_INTERVAL);
	}

	private static CageTier tierOf(BlockState state) {
		return state.getBlock() instanceof MonsterCageBlock cage ? cage.tier() : CageTier.ANDESITE;
	}

	private static boolean isFlooded(BlockState state) {
		return state.getBlock() instanceof MonsterCageBlock cage && cage.isFlooded();
	}

	public CageTier getTier() {
		return tierOf(getBlockState());
	}

	public boolean isFlooded() {
		return isFlooded(getBlockState());
	}

	public boolean isNetherite() {
		return getTier() == CageTier.NETHERITE;
	}

	public int getCapacity() {
		return getTier().capacity();
	}

	public int getMonstrousLevel() {
		return MachineEnchantments.levelIn(enchantments, MachineEnchantments.MONSTROUS);
	}

	// ---------------------------------------------------------------- the monster

	public boolean hasMonster() {
		return !captive.isEmpty() && count > 0;
	}

	public int getCount() {
		return count;
	}

	public int getArmed() {
		return armed;
	}

	public ItemStack getCaptive() {
		return captive;
	}

	@Nullable
	public EntityType<?> getMonsterType() {
		return monsterType;
	}

	/** Every change of the captive comes through here, so its monster's type is read once per jar. */
	private void holdCaptive(ItemStack jar) {
		captive = jar;
		monsterType = MonsterJarItem.typeOf(jar);
		createFailed = false;
	}

	/**
	 * One of the monsters, not in the world: what it drops and sounds like. Bare (D27), and slimes and magma cubes at
	 * their smallest size that drops anything.
	 */
	@Nullable
	public Entity sample() {
		if (sample == null && hasMonster() && level != null) {
			sample = MonsterJarItem.create(captive, level).orElse(null);
			if (sample instanceof MagmaCube cube)
				cube.setSize(2, true);
			else if (sample instanceof Slime slime)
				slime.setSize(1, true);
			if (sample instanceof Mob mob)
				for (var slot : net.minecraft.world.entity.EquipmentSlot.values())
					mob.setItemSlot(slot, ItemStack.EMPTY);
			if (sample != null)
				sample.setPos(Vec3.atCenterOf(worldPosition));
		}
		return sample;
	}

	public boolean isChow(ItemStack stack) {
		return stack.is(ModItems.MONSTER_CHOW.get());
	}

	public int getChowCount() {
		if (showsSummary())
			return shownChowCount;
		int n = 0;
		for (int i = 0; i < chow.getSlots(); i++)
			n += chow.getStackInSlot(i).getCount();
		return n;
	}

	/** Clients go by the server's summary of the bowl and the tray (see {@link #writeSummary}), not the inventories. */
	boolean showsSummary() {
		return level != null && level.isClientSide;
	}

	/** Monster Chow a new one of this monster eats. */
	public int chowCost() {
		EntityType<?> type = getMonsterType();
		return type == null ? 0 : CageRules.chowCost(type);
	}

	private void eat(int amount) {
		for (int i = 0; i < chow.getSlots() && amount > 0; i++)
			amount -= chow.extractItem(i, amount, false).getCount();
	}

	/** Ponder only: this monster, so many of them (armed drowned among them). */
	public void setPonderHorde(ItemStack jar, int monsters, int armedOnes) {
		if (!ItemStack.isSameItemSameComponents(jar, captive) || armedOnes != armed) {
			sample = null;
			shown.clear();
		}
		holdCaptive(jar.copyWithCount(1));
		count = jar.isEmpty() ? 0 : monsters;
		armed = Math.min(armedOnes, count);
	}

	/** Ponder only: what's in the bowl, and a Flooded cage's water. */
	public void setPonderSupplies(ItemStack food, int waterMb) {
		for (int i = 0; i < chow.getSlots(); i++)
			chow.setStackInSlot(i, i == 0 ? food.copy() : ItemStack.EMPTY);
		water.setFluid(waterMb > 0 ? new FluidStack(Fluids.WATER, Math.min(waterMb, water.getCapacity())) : FluidStack.EMPTY);
	}

	/** Ponder only: the Netherite cage's field up, or down with so many ticks to the next escape. */
	public void setPonderField(boolean up, int ticksToEscape) {
		fieldUp = up;
		escapeIn = up ? -1 : ticksToEscape;
		paused = false;
	}

	private void setCaptive(ItemStack jar) {
		// the first caged monster, and the first caged Wither: to whoever's near, however the jar went in
		if (!jar.isEmpty() && level != null && !level.isClientSide) {
			ModAdvancements.awardNearby(level, worldPosition, ModAdvancements.CAGE_MONSTER, ItemStack.EMPTY, 1);
			if (CageRules.kindOf(MonsterJarItem.typeOf(jar)) == CageRules.Kind.WITHER)
				ModAdvancements.awardNearby(level, worldPosition, ModAdvancements.CAGE_WITHER, ItemStack.EMPTY, 1);
		}
		holdCaptive(jar);
		count = jar.isEmpty() ? 0 : 1;
		armed = 0;
		progress = 0;
		drownTicks = 0;
		sample = null;
		shown.clear();
		if (jar.isEmpty())
			escapeIn = -1;
		setChanged();
		sendData();
		if (level != null)
			level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
	}

	/** Takes the jarred monster back out; the rest are culled into the output. */
	private ItemStack release() {
		if (!hasMonster())
			return ItemStack.EMPTY;
		if (level instanceof ServerLevel server)
			store(cull(server, count - 1, null, ItemStack.EMPTY));
		ItemStack out = captive;
		setCaptive(ItemStack.EMPTY);
		return out;
	}

	// ---------------------------------------------------------------- the Netherite cage: owner and lock-down

	public void setOwner(Player player) {
		if (!isNetherite())
			return;
		owner = player.getUUID();
		ownerName = player.getGameProfile().getName();
		setChanged();
		sendData();
	}

	public String getOwnerName() {
		return ownerName;
	}

	/** Its owner, their team, or an operator in creative; anyone at all for the other cages. */
	public boolean mayManage(Player player) {
		if (!isNetherite() || owner == null || owner.equals(player.getUUID()))
			return true;
		if (player.isCreative() && player.hasPermissions(2))
			return true;
		Player ownerPlayer = level == null ? null : level.getPlayerByUUID(owner);
		return ownerPlayer != null && player.isAlliedTo(ownerPlayer);
	}

	/** Nobody can break or empty a Netherite cage while its field is down with a Wither in it (D41). */
	public boolean isLockedDown() {
		return isNetherite() && hasMonster() && !fieldUp;
	}

	public boolean mayBreak(Player player) {
		return !isLockedDown() && mayManage(player);
	}

	public Component whyNotBreak(Player player) {
		return isLockedDown() ? tr("locked_down") : tr("not_owner", ownerName);
	}

	// ---------------------------------------------------------------- using it

	/** A click with an item (by a player or a Deployer); false if the item does nothing here. */
	boolean use(Player player, InteractionHand hand, ItemStack stack) {
		boolean client = level == null || level.isClientSide;
		if (!hasMonster()) {
			if (!MonsterJarItem.isFilled(stack))
				return false;
			if (!client) {
				EntityType<?> type = MonsterJarItem.typeOf(stack);
				if (!mayManage(player))
					player.displayClientMessage(tr("not_owner", ownerName), true);
				else if (type == null || !CageRules.fits(getTier(), isFlooded(), type))
					player.displayClientMessage(tr("wrong_cage", type == null ? Component.empty() : type.getDescription()), true);
				else {
					// the jar goes in with its monster and comes back out with it (handing back an empty one duplicated jars)
					setCaptive(stack.split(1));
					level.playSound(null, worldPosition, ModSounds.cage("jar_in"), SoundSource.BLOCKS, 1, 1);
				}
			}
			return true;
		}
		if (stack.is(ItemTags.SWORDS)) {
			if (!client)
				swipe(player, hand, stack);
			return true;
		}
		if (isChow(stack)) {
			if (!client) {
				ItemStack left = ItemHandlerHelper.insertItemStacked(chow, stack.copy(), false);
				if (!player.getAbilities().instabuild)
					stack.setCount(left.getCount());
			}
			return true;
		}
		return false;
	}

	/** Half the horde (rounded, always leaving one) is culled by whoever holds the sword; armed drowned go first. */
	private void swipe(Player player, InteractionHand hand, ItemStack sword) {
		if (!(level instanceof ServerLevel server))
			return;
		if (isLockedDown()) {
			player.displayClientMessage(tr("locked_down"), true);
			return;
		}
		int killed = Math.min(Math.round(count / 2f), count - 1);
		if (killed <= 0) {
			player.displayClientMessage(tr("last_one"), true);
			return;
		}
		store(cull(server, killed, player, sword));
		if (!(player instanceof FakePlayer)) {
			int xp = 0;
			for (int i = 0; i < killed; i++)
				xp += sample() instanceof LivingEntity living ? living.getExperienceReward(server, player) : 0;
			ExperienceOrb.award(server, frontOf(0.3), xp);
		}
		sword.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
		Vec3 at = frontOf(0.5);
		server.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 1, 0, 0, 0, 0);
		server.playSound(null, worldPosition, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1, 1);
		server.playSound(null, worldPosition, ModSounds.cage("bars_rattle"), SoundSource.BLOCKS, 1, 1);
		setChanged();
		sendData();
		server.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
	}

	/**
	 * The drops of {@code killed} monsters from the horde, as if killed by {@code killer} with {@code weapon} (Looting,
	 * Fire Aspect), at vanilla rates. Armed drowned go first, each dropping its trident now and then (D60); each Wither
	 * leaves its Nether Star, which vanilla drops outside its loot table.
	 */
	private List<ItemStack> cull(ServerLevel server, int killed, @Nullable Player killer, ItemStack weapon) {
		List<ItemStack> drops = new ArrayList<>();
		if (killed <= 0 || !(sample() instanceof LivingEntity monster))
			return drops;
		killed = Math.min(killed, count - 1);
		LootTable table = server.getServer().reloadableRegistries().getLootTable(monster.getLootTable());
		DamageSource source = killer != null ? server.damageSources().playerAttack(killer) : server.damageSources().generic();
		var enchantments = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		boolean burning = !weapon.isEmpty()
			&& EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.FIRE_ASPECT), weapon) > 0;
		int looting = weapon.isEmpty() ? 0
			: EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.LOOTING), weapon);
		if (burning)
			monster.igniteForSeconds(4);
		for (int i = 0; i < killed; i++) {
			LootParams.Builder params = new LootParams.Builder(server)
				.withParameter(LootContextParams.THIS_ENTITY, monster)
				.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
				.withParameter(LootContextParams.DAMAGE_SOURCE, source);
			if (killer != null)
				params.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer)
					.withParameter(LootContextParams.ATTACKING_ENTITY, killer)
					.withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, killer)
					.withLuck(killer.getLuck());
			drops.addAll(table.getRandomItems(params.create(LootContextParamSets.ENTITY)));
			if (monster instanceof WitherBoss)
				drops.add(new ItemStack(Items.NETHER_STAR));
		}
		if (burning)
			monster.clearFire();
		int armedKilled = Math.min(armed, killed);
		for (int i = 0; i < armedKilled; i++)
			if (server.random.nextFloat() < TRIDENT_DROP + 0.01f * looting) {
				ItemStack trident = new ItemStack(Items.TRIDENT);
				trident.setDamageValue(server.random.nextInt(Math.max(1, trident.getMaxDamage() - 3)));
				drops.add(trident);
			}
		armed -= armedKilled;
		count -= killed;
		// up to 255 rolls: joined into full stacks before they're stored or dropped
		return Drops.merged(drops);
	}

	/** Into the output; what doesn't fit drops out of the front. */
	private void store(List<ItemStack> drops) {
		boolean any = false;
		for (ItemStack drop : drops) {
			ItemStack left = ItemHandlerHelper.insertItemStacked(output, drop, false);
			any |= left.getCount() < drop.getCount();
			if (!left.isEmpty() && level != null) {
				Vec3 at = frontOf(0.6);
				Containers.dropItemStack(level, at.x, at.y, at.z, left);
			}
		}
		if (any && level != null && level.getGameTime() - lastDrop > 10) {
			lastDrop = level.getGameTime();
			level.playSound(null, worldPosition, ModSounds.cage("drop_tray"), SoundSource.BLOCKS, 1, 1);
		}
	}

	private Vec3 frontOf(double out) {
		Direction front = getBlockState().getValue(MonsterCageBlock.FACING);
		return Vec3.atCenterOf(worldPosition).add(Vec3.atLowerCornerOf(front.getNormal()).scale(out));
	}

	// ---------------------------------------------------------------- running

	@Override
	public void tick() {
		super.tick();
		if (level == null)
			return;
		if (level.isClientSide) {
			clientTick();
			return;
		}
		float wasImpact = impact.applied();
		impact.settle(wantedImpact());
		// the goggles show the settled impact, so clients hear when it changes, network or not
		if (Float.compare(wasImpact, impact.applied()) != 0)
			syncQueued = true;
		if (FixedStress.keep(this, impact()))
			sendData();
		if (isNetherite()) {
			field();
			energyUpkeep();
		}
		if (isFlooded())
			flooded();
		if (canGrow())
			grow();
		grumble();
		// sendData clears the queue, so a sync this tick already sent leaves nothing to send again
		if (syncQueued && SyncStagger.due(level.getGameTime(), syncPhase, SYNC_INTERVAL))
			sendData();
	}

	@Override
	public void sendData() {
		if (level != null) {
			long now = level.getGameTime();
			ownSyncAt = sentEnergyAt = now;
			markCaptiveSent(now);
		}
		sentEnergy = energy.getEnergyStored();
		syncQueued = false;
		super.sendData();
	}

	/** Turning, not overstressed, holding a monster, and (Netherite) its field up and farming powered. */
	public boolean isRunning() {
		return getSpeed() != 0 && !isOverStressed() && hasMonster() && (!isNetherite() || fieldUp && farmingPowered);
	}

	private boolean peaceful() {
		return level != null && level.getDifficulty() == Difficulty.PEACEFUL;
	}

	public boolean isDrowning() {
		return isFlooded() && hasMonster() && getMonsterType() != null && CageRules.drowns(getMonsterType());
	}

	/** Everything a new monster needs. */
	private boolean canGrow() {
		return isRunning() && !peaceful() && !isDrowning() && count < getCapacity() && getChowCount() >= chowCost()
			&& (!isFlooded() || water.getFluidAmount() >= waterPerMonster());
	}

	public boolean isGrowing() {
		return canGrow();
	}

	private int waterPerMonster() {
		return CMConfig.Server.get(CMConfig.SERVER.cageWaterPerMonster);
	}

	/** Ticks per new monster at the reference speed, sped up by Monstrous. */
	public float interval() {
		CageTier tier = getTier();
		float base = 20f * tier.baseSeconds() * (tier == CageTier.NETHERITE ? 1 : Math.max(1, chowCost()));
		return Math.max(1, base / MachineEnchantments.speedMultiplier(getMonstrousLevel(), CMConfig.SERVER.cageMonstrousBonus));
	}

	/** Seconds per new monster at the current speed (0 when stopped). */
	public float secondsPerMonster() {
		float speed = Math.abs(getSpeed());
		return speed == 0 ? 0 : interval() * getTier().referenceRpm() / speed / 20f;
	}

	private void grow() {
		progress += Math.abs(getSpeed()) / getTier().referenceRpm();
		if (progress < interval())
			return;
		progress = 0;
		eat(chowCost());
		if (isFlooded())
			water.drain(waterPerMonster(), IFluidHandler.FluidAction.EXECUTE);
		count++;
		if (getMonsterType() == EntityType.DROWNED && level.random.nextFloat() < ARMED_CHANCE)
			armed++;
		level.playSound(null, worldPosition, ModSounds.cage("arrival"), SoundSource.BLOCKS, 1, 0.95f + 0.1f * level.random.nextFloat());
		setChanged();
		sendData();
		level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
	}

	/** Progress to the next monster, 0 to 1. */
	public float getProgress() {
		return Mth.clamp(progress / interval(), 0, 1);
	}

	@Override
	public float calculateStressApplied() {
		float impact = impact();
		lastStressApplied = impact;
		return impact;
	}

	/** The impact the network should hold (see {@link #impact}); clients just go by what's wanted. */
	private float impact() {
		if (level == null || level.isClientSide)
			return Float.isNaN(syncedImpact) ? wantedImpact() : syncedImpact;
		float applied = impact.applied();
		return Float.isNaN(applied) ? wantedImpact() : applied;
	}

	/** Per RPM while it works; nothing while empty, full, out of chow or water, or with the field down. */
	private float wantedImpact() {
		boolean working = hasMonster() && !peaceful() && count < getCapacity() && getChowCount() >= chowCost()
			&& (!isFlooded() || water.getFluidAmount() >= waterPerMonster()) && (!isNetherite() || fieldUp);
		return working ? (float) getTier().stressPerRpm() : 0;
	}

	@Override
	public void onSpeedChanged(float previousSpeed) {
		super.onSpeedChanged(previousSpeed);
		if (level != null && !level.isClientSide && FixedStress.keep(this, impact()))
			sendData();
	}

	// ---------------------------------------------------------------- the Flooded cages

	private void flooded() {
		// a zombie (or husk) drowning, while there's water
		if (isDrowning() && water.getFluidAmount() > 0) {
			if (++drownTicks >= DROWN_TICKS)
				drown();
		} else {
			drownTicks = 0;
		}
		// the shut of the hatch after a throw
		if (hatchShutIn > 0 && --hatchShutIn == 0)
			level.playSound(null, worldPosition, ModSounds.cage("hatch_shut"), SoundSource.BLOCKS, 1, 1);
		if (throwCooldown > 0)
			throwCooldown--;
		else if (armed > 0 && getMonsterType() == EntityType.DROWNED && !peaceful())
			throwTrident();
	}

	/** Zombie into drowned, husk into zombie (then on to drowned), as in vanilla. */
	private void drown() {
		EntityType<?> from = getMonsterType();
		EntityType<?> to = from == EntityType.HUSK ? EntityType.ZOMBIE : EntityType.DROWNED;
		CustomData data = captive.get(DataComponents.ENTITY_DATA);
		CompoundTag tag = data == null ? new CompoundTag() : data.copyTag();
		tag.putString("id", EntityType.getKey(to).toString());
		for (String key : new String[] {"DrownedConversionTime", "InWaterTime", "Health", "attributes"})
			tag.remove(key);
		ItemStack jar = captive.copy();
		jar.set(DataComponents.ENTITY_DATA, CustomData.of(tag));
		holdCaptive(jar);
		sample = null;
		shown.clear();
		drownTicks = 0;
		level.playSound(null, worldPosition, from == EntityType.HUSK ? SoundEvents.HUSK_CONVERTED_TO_ZOMBIE
			: SoundEvents.ZOMBIE_CONVERTED_TO_DROWNED, SoundSource.HOSTILE, 1, 1);
		setChanged();
		sendData();
	}

	/** An armed drowned throws a real trident out of the front at the nearest player in sight (D61). */
	private void throwTrident() {
		Direction front = getBlockState().getValue(MonsterCageBlock.FACING);
		Vec3 hatch = Vec3.atCenterOf(worldPosition).add(Vec3.atLowerCornerOf(front.getNormal()).scale(0.65)).add(0, 0.3, 0);
		Player target = null;
		double best = THROW_RANGE * THROW_RANGE;
		for (Player player : level.players()) {
			if (player.isSpectator() || player.isCreative())
				continue;
			Vec3 to = player.getEyePosition().subtract(hatch);
			double d = to.lengthSqr();
			if (d > best || to.normalize().dot(Vec3.atLowerCornerOf(front.getNormal())) < 0.2)
				continue;
			HitResult hit = level.clip(new ClipContext(hatch, player.getEyePosition(), ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, CollisionContext.empty()));
			if (hit.getType() != HitResult.Type.MISS)
				continue;
			best = d;
			target = player;
		}
		if (target == null) {
			// nobody to throw at: look again in a moment, not every tick
			throwCooldown = SEARCH_COOLDOWN;
			return;
		}
		throwCooldown = THROW_COOLDOWN;
		ThrownTrident trident = new ThrownTrident(level, hatch.x, hatch.y, hatch.z, new ItemStack(Items.TRIDENT));
		trident.pickup = AbstractArrow.Pickup.DISALLOWED;
		double dx = target.getX() - hatch.x, dz = target.getZ() - hatch.z;
		double dy = target.getY(0.33) - hatch.y;
		trident.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.2, dz, 1.6f, 14 - level.getDifficulty().getId() * 4);
		level.addFreshEntity(trident);
		level.playSound(null, worldPosition, ModSounds.cage("hatch_open"), SoundSource.BLOCKS, 1, 1);
		level.playSound(null, worldPosition, SoundEvents.DROWNED_SHOOT, SoundSource.HOSTILE, 1, 1);
		level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
		hatchShutIn = HATCH_SHUT_TICKS;
	}

	@Override
	public boolean triggerEvent(int id, int param) {
		if (id == 1) {
			if (level != null && level.isClientSide)
				hatchOpenedAt = level.getGameTime();
			return true;
		}
		return false;
	}

	/** Ticks since the hatch last flapped open (client), for the renderer. */
	public long ticksSinceHatch() {
		return level == null ? Long.MAX_VALUE : level.getGameTime() - hatchOpenedAt;
	}

	public FluidTank getWater() {
		return water;
	}

	/** What pipes see of a Flooded cage's water: it goes in, nothing comes out. */
	public IFluidHandler getWaterHandler() {
		return waterIn;
	}

	// ---------------------------------------------------------------- the Netherite cage's field

	private int warningTicks() {
		return 20 * CMConfig.Server.get(CMConfig.SERVER.cageWarningSeconds);
	}

	/** Takes the field's FE, and runs the countdown and the escapes while it's down. */
	private void field() {
		int wasEscapeIn = escapeIn;
		boolean wasPaused = paused;
		runField();
		// the countdown is saved with the chunk as it runs, or a crash or an unload would put it back
		if (escapeIn != wasEscapeIn || paused != wasPaused)
			level.blockEntityChanged(worldPosition);
	}

	private void runField() {
		long received = receivedNow;
		receivedNow = 0;
		noteOffers();
		if (!hasMonster()) {
			if (fieldUp || startingFor >= 0)
				switchOff();
			escapeIn = -1;
			return;
		}
		boolean was = fieldUp, wasFarming = farmingPowered;
		int fieldCost = CMConfig.Server.get(CMConfig.SERVER.cageFieldFe);
		int farmCost = getSpeed() == 0 || count >= getCapacity() ? 0
			: Math.round(CMConfig.Server.get(CMConfig.SERVER.cageFarmFe) * Math.min(1, Math.abs(getSpeed()) / 256f));
		if (fieldUp) {
			// up: as ever, the field first and farming shed first; stored FE covers dips, and once it can't, it's off
			if (energy.getEnergyStored() >= fieldCost) {
				energy.take(fieldCost);
				farmingPowered = energy.getEnergyStored() >= farmCost;
				if (farmingPowered)
					energy.take(farmCost);
			} else {
				switchOff();
			}
		} else if (startingFor >= 0) {
			starting(received, fieldCost);
		} else if (offeredTicks >= SUPPLY_TICKS && offeredSum >= (long) SUPPLY_TICKS * fieldCost && level.getGameTime() >= retryAt) {
			// a second's offers cover the field: a trial, taking FE again, and up once that FE arrives
			startingFor = 0;
			startingFull = 0;
		}
		if (fieldUp != was) {
			level.playSound(null, worldPosition, ModSounds.cage(fieldUp ? "field_up" : "field_down"), SoundSource.BLOCKS, 1, 1);
			sendData();
		} else if (farmingPowered != wasFarming) {
			// queued: on a supply just short of both draws this can flip every tick
			syncQueued = true;
		}
		if (fieldUp) {
			// holding: a countdown stands still, and is called off once the field has held for the hold
			if (fieldUp != was && escapeIn >= 0 && escapeIn <= warningTicks())
				announce("alarm_holding", time(holdTicks()));
			if (++heldFor >= holdTicks()) {
				retryWait = 0;
				if (escapeIn >= 0) {
					if (escapeIn <= warningTicks())
						announce("alarm_restored");
					escapeIn = -1;
					paused = false;
					sendData();
				}
			}
			return;
		}
		if (peaceful() || !CMConfig.Server.get(CMConfig.SERVER.cageEscapes))
			return;
		if (escapeIn < 0) {
			escapeIn = warningTicks();
			paused = !playerNear();
			if (!paused)
				announce("alarm_start", time(escapeIn));
			sendData();
		}
		boolean near = playerNear();
		if (!near) {
			if (!paused) {
				paused = true;
				sendData();
			}
			return;
		}
		if (paused) {
			paused = false;
			if (escapeIn <= warningTicks())
				announce("alarm_remind", time(escapeIn));
			sendData();
		}
		escapeIn--;
		if (escapeIn <= warningTicks())
			warn();
		if (escapeIn <= 0)
			escape();
		else if (escapeIn % 20 == 0)
			syncQueued = true;
	}

	/** This tick's offers into the last second's, while the field is off (they don't count while it's on). */
	private void noteOffers() {
		long now = offeredNow;
		offeredNow = 0;
		if (fieldUp || startingFor >= 0)
			return;
		offeredSum += now - offered[offeredAt];
		offered[offeredAt] = now;
		offeredAt = (offeredAt + 1) % SUPPLY_TICKS;
		offeredTicks = Math.min(SUPPLY_TICKS, offeredTicks + 1);
	}

	/**
	 * A tick of a trial: nothing is drawn, the FE coming in is only measured (and kept), and the field comes up after a
	 * second in which every tick brought in its cost; what came in is then its reserve. A tick short, or no FE at all
	 * within {@link #START_WAIT}, and it's off again, quietly, keeping what came in, and the next trial waits.
	 */
	private void starting(long received, int fieldCost) {
		startingFor++;
		if (startingFull == 0 && received == 0) {
			if (startingFor > START_WAIT)
				failTrial();
			return;
		}
		if (received < fieldCost) {
			failTrial();
			return;
		}
		if (++startingFull >= SUPPLY_TICKS) {
			startingFor = -1;
			fieldUp = true;
			farmingPowered = false;
			heldFor = 0;
		}
	}

	private void failTrial() {
		retryWait = retryWait == 0 ? RETRY_FIRST : Math.min(RETRY_MOST, 2 * retryWait);
		retryAt = level.getGameTime() + retryWait;
		switchOff();
	}

	/** Ticks the field must hold before a countdown is called off. */
	private static int holdTicks() {
		return 20 * CMConfig.Server.get(CMConfig.SERVER.cageRestoreHoldSeconds);
	}

	/** Off: it takes no more FE, and only a fresh second of offers covering the cost starts it again. */
	private void switchOff() {
		fieldUp = false;
		heldFor = 0;
		startingFor = -1;
		startingFull = 0;
		java.util.Arrays.fill(offered, 0);
		offeredSum = 0;
		offeredAt = 0;
		offeredTicks = 0;
	}

	/** Whether the field takes FE: up, or starting. */
	private boolean takesFe() {
		return fieldUp || startingFor >= 0;
	}

	/**
	 * After the field's draw: the FE is saved with the chunk and reaches clients now and then (not a sync and a comparator
	 * update every tick), and comparators hear when a countdown starts or ends.
	 */
	private void energyUpkeep() {
		if (energyChanged) {
			energyChanged = false;
			level.blockEntityChanged(worldPosition);
		}
		if (energy.getEnergyStored() != sentEnergy && level.getGameTime() - sentEnergyAt >= ENERGY_SYNC_TICKS)
			syncQueued = true;
		boolean warning = isWarning();
		if (warning != toldWarning) {
			toldWarning = warning;
			level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
		}
	}

	/** The messages, the klaxon and the field sputtering, through the warning (D75, D77). */
	private void warn() {
		int warning = warningTicks();
		if (escapeIn == warning)
			announce("alarm_start", time(escapeIn));
		else if (escapeIn > 0 && (escapeIn % 1200 == 0 || escapeIn == 600))
			announce("alarm_remind", time(escapeIn));
		if (escapeIn <= 600 && escapeIn % 20 == 0)
			for (ServerPlayer player : playersNear())
				player.displayClientMessage(tr("alarm_countdown", time(escapeIn)).withStyle(ChatFormatting.RED), true);
		// three blasts at the start and every 30 s, every 10 s in the last minute, continuously in the last 10 s
		int elapsed = warning - escapeIn;
		boolean blast;
		if (escapeIn <= 200)
			blast = escapeIn % BLAST_GAP == 0;
		else if (escapeIn <= 1200)
			blast = (1200 - escapeIn) % 200 < 3 * BLAST_GAP && (1200 - escapeIn) % 200 % BLAST_GAP == 0;
		else
			blast = elapsed % 600 < 3 * BLAST_GAP && elapsed % 600 % BLAST_GAP == 0;
		if (blast)
			level.playSound(null, worldPosition, ModSounds.cage("klaxon"), SoundSource.BLOCKS, 2, 1);
		if (level.random.nextInt(25) == 0)
			level.playSound(null, worldPosition, ModSounds.cage("field_flicker"), SoundSource.BLOCKS, 1,
				0.9f + 0.2f * level.random.nextFloat());
	}

	/** One Wither breaks out at the nearest open space; the next a game day later (D36, D38, D43). */
	private void escape() {
		if (!(level instanceof ServerLevel server))
			return;
		WitherBoss wither = EntityType.WITHER.create(server);
		if (wither == null)
			return;
		Vec3 at = openSpace(server, wither);
		wither.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
		wither.makeInvulnerable();
		server.addFreshEntity(wither);
		server.playSound(null, worldPosition, ModSounds.cage("cage_burst"), SoundSource.BLOCKS, 2, 1);
		announce("alarm_escaped");
		count--;
		if (count <= 0) {
			// the last one out: the empty jar is left behind
			ItemStack jar = captive.getItem() instanceof MonsterJarItem item ? item.empty() : ItemStack.EMPTY;
			setCaptive(ItemStack.EMPTY);
			store(List.of(jar));
			return;
		}
		escapeIn = 20 * CMConfig.Server.get(CMConfig.SERVER.cageEscapeSeconds);
		setChanged();
		sendData();
		server.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
	}

	/** The nearest spot around the cage a Wither fits (by its box), searching outwards; above the cage if none. */
	private Vec3 openSpace(ServerLevel server, Entity wither) {
		Direction front = getBlockState().getValue(MonsterCageBlock.FACING);
		Vec3 best = null;
		double bestD = Double.MAX_VALUE;
		Vec3 centre = Vec3.atBottomCenterOf(worldPosition);
		for (int r = 1; r <= 4 && best == null; r++)
			for (int dx = -r; dx <= r; dx++)
				for (int dy = -1; dy <= r; dy++)
					for (int dz = -r; dz <= r; dz++) {
						Vec3 at = centre.add(dx, dy, dz);
						AABB box = wither.getType().getSpawnAABB(at.x, at.y, at.z);
						if (!server.noCollision(box))
							continue;
						// prefer the front
						double d = at.distanceToSqr(centre) - 0.5 * (dx * front.getStepX() + dz * front.getStepZ());
						if (d < bestD) {
							bestD = d;
							best = at;
						}
					}
		return best != null ? best : centre.add(0, 1, 0);
	}

	private boolean playerNear() {
		return !playersNear().isEmpty();
	}

	private List<ServerPlayer> playersNear() {
		List<ServerPlayer> near = new ArrayList<>();
		if (!(level instanceof ServerLevel server))
			return near;
		double range = CMConfig.Server.get(CMConfig.SERVER.cageAlarmRange);
		for (ServerPlayer player : server.players())
			if (!player.isSpectator() && player.distanceToSqr(Vec3.atCenterOf(worldPosition)) <= range * range)
				near.add(player);
		return near;
	}

	/** A chat message to everyone within range (D75). */
	private void announce(String key, Object... args) {
		Object[] all = new Object[args.length + 1];
		all[0] = worldPosition.getX() + ", " + worldPosition.getY() + ", " + worldPosition.getZ();
		System.arraycopy(args, 0, all, 1, args.length);
		Component message = tr(key, all).withStyle(key.equals("alarm_restored") ? ChatFormatting.AQUA : ChatFormatting.RED);
		for (ServerPlayer player : playersNear())
			player.sendSystemMessage(message);
	}

	private static String time(int ticks) {
		int s = Math.max(0, ticks) / 20;
		return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
	}

	public boolean isFieldUp() {
		return fieldUp;
	}

	/** Counting down to an escape (the warning), as opposed to waiting out the day after one, or nothing at all. */
	public boolean isWarning() {
		return escapeIn >= 0 && escapeIn <= warningTicks() && !fieldUp;
	}

	public int getEscapeIn() {
		return escapeIn;
	}

	public boolean isPaused() {
		return paused;
	}

	public IEnergyStorage getEnergy() {
		return energy;
	}

	public int getEnergyStored() {
		return energy.getEnergyStored();
	}

	public int getEnergyCapacity() {
		return energy.getMaxEnergyStored();
	}

	// ---------------------------------------------------------------- the stored monster's own voice

	/** Shared quiet: the last grumble near each place, so a wall of cages never talks over itself. */
	private static final Map<ResourceKey<Level>, List<long[]>> RECENT_GRUMBLES = new HashMap<>();
	@Nullable
	private static Method ambientSound;

	/** Now and then (every 10-30 s), quietly, the stored monster's own ambient sound (D: user, quieter). */
	private void grumble() {
		if (!hasMonster() || --grumbleIn > 0)
			return;
		grumbleIn = 200 + level.random.nextInt(400);
		List<long[]> recent = RECENT_GRUMBLES.computeIfAbsent(level.dimension(), k -> new ArrayList<>());
		long now = level.getGameTime();
		recent.removeIf(e -> now - e[1] > 200);
		for (long[] e : recent)
			if (BlockPos.of(e[0]).distSqr(worldPosition) <= 64)
				return;
		SoundEvent sound = ambientOf(sample());
		if (sound == null)
			return;
		recent.add(new long[] {worldPosition.asLong(), now});
		level.playSound(null, worldPosition, sound, SoundSource.HOSTILE,
			(float) CMConfig.Server.get(CMConfig.SERVER.cageGrumbleVolume), 0.95f + 0.1f * level.random.nextFloat());
	}

	@Nullable
	private static SoundEvent ambientOf(@Nullable Entity entity) {
		if (!(entity instanceof Mob mob))
			return null;
		try {
			if (ambientSound == null) {
				ambientSound = Mob.class.getDeclaredMethod("getAmbientSound");
				ambientSound.setAccessible(true);
			}
			return (SoundEvent) ambientSound.invoke(mob);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}

	// ---------------------------------------------------------------- the window (client)

	public List<CageMonster> getShown() {
		return shown;
	}

	private void clientTick() {
		if (canGrow())
			progress = Math.min(progress + Math.abs(getSpeed()) / getTier().referenceRpm(), interval() - 1);
		// the drowning counts on here too, between syncs (the server only sends the conversion itself)
		if (isDrowning() && water.getFluidAmount() > 0)
			drownTicks = Math.min(drownTicks + 1, DROWN_TICKS - 1);
		else
			drownTicks = 0;
		// the monsters in the window are only built near enough to be seen soon, and let go well out of sight
		viewDistanceSqr = ClientView.distanceSqr.applyAsDouble(this);
		int want = ClientView.keep(hasMonster() ? Math.min(count, isNetherite() ? 1 : SHOWN) : 0, viewDistanceSqr);
		while (shown.size() > want)
			shown.removeLast();
		if (ClientView.builds(viewDistanceSqr))
			buildShown(want, mayBuild);
		for (CageMonster monster : shown)
			monster.tick(level.random, isRunning() || getSpeed() != 0);
		clientTicker.accept(this);
	}

	/**
	 * Builds the window's monsters up to {@code want}, each one {@code mayBuild} allows. A jar whose monster can't be
	 * made stops it trying (and taking builds) until the jar changes.
	 */
	void buildShown(int want, BooleanSupplier mayBuild) {
		while (shown.size() < want && !createFailed && mayBuild.getAsBoolean()) {
			Entity made = MonsterJarItem.create(captive, level).orElse(null);
			if (made == null) {
				createFailed = true;
				return;
			}
			if (made instanceof Mob mob)
				for (var slot : net.minecraft.world.entity.EquipmentSlot.values())
					mob.setItemSlot(slot, ItemStack.EMPTY);
			// an armed drowned holds its trident up
			if (made instanceof Mob mob && shown.size() < armed && getMonsterType() == EntityType.DROWNED)
				mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
			shown.add(new CageMonster(made, level.random, isNetherite()));
		}
	}

	/** Client: the camera's squared distance as of this tick (the loops go by it too). */
	public double getViewDistanceSqr() {
		return viewDistanceSqr;
	}

	// ---------------------------------------------------------------- the wrench screen and readouts

	@Override
	public Component getDisplayName() {
		return getBlockState().getBlock().getName();
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new MonsterCageMenu(containerId, inventory, this);
	}

	@Override
	public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
		buffer.writeBlockPos(worldPosition);
	}

	public IItemHandlerModifiable getCaptiveSlot() {
		return captiveSlot;
	}

	public IItemHandlerModifiable getChow() {
		return chow;
	}

	public IItemHandlerModifiable getOutput() {
		return output;
	}

	/** The chow to show in the bowl: some once there's any. */
	public ItemStack getBowlChow() {
		if (showsSummary())
			return shownChow;
		for (int i = 0; i < chow.getSlots(); i++)
			if (!chow.getStackInSlot(i).isEmpty())
				return chow.getStackInSlot(i);
		return ItemStack.EMPTY;
	}

	/** The first few stacks waiting in the output, for the tray. */
	public List<ItemStack> getTray() {
		return tray;
	}

	/** Output slots in use, for how full the tray looks. */
	public int getTrayUsed() {
		return trayUsed;
	}

	/** The box the renderer draws in: the shield sits a pixel outside the block. Made once, not every frame. */
	public AABB getRenderBox() {
		if (renderBox == null)
			renderBox = new AABB(worldPosition).inflate(0.1);
		return renderBox;
	}

	/** The bowl and the tray worked out again from the inventories (on the server, or where a client has them). */
	private void summarise() {
		shownChow = ItemStack.EMPTY;
		shownChowCount = 0;
		for (int i = 0; i < chow.getSlots(); i++) {
			ItemStack stack = chow.getStackInSlot(i);
			if (shownChow.isEmpty() && !stack.isEmpty())
				shownChow = stack.copyWithCount(1);
			shownChowCount += stack.getCount();
		}
		tray.clear();
		trayUsed = 0;
		for (int i = 0; i < output.getSlots(); i++) {
			ItemStack stack = output.getStackInSlot(i);
			if (stack.isEmpty())
				continue;
			trayUsed++;
			if (tray.size() < TRAY_SHOWN)
				tray.add(stack.copyWithCount(1));
		}
	}

	/** What clients are sent of the bowl and the tray: all the renderer, goggles and screen read of them. */
	private void writeSummary(CompoundTag tag, HolderLookup.Provider registries) {
		summarise();
		if (!shownChow.isEmpty())
			tag.put("ShownChow", shownChow.save(registries));
		tag.putInt("ChowCount", shownChowCount);
		ListTag shownTray = new ListTag();
		for (ItemStack stack : tray)
			shownTray.add(stack.save(registries));
		tag.put("Tray", shownTray);
		tag.putInt("TrayUsed", trayUsed);
	}

	private void readSummary(CompoundTag tag, HolderLookup.Provider registries) {
		shownChow = ItemStack.parseOptional(registries, tag.getCompound("ShownChow"));
		shownChowCount = tag.getInt("ChowCount");
		tray.clear();
		for (Tag entry : tag.getList("Tray", Tag.TAG_COMPOUND))
			ItemStack.parse(registries, entry).ifPresent(tray::add);
		trayUsed = tag.getInt("TrayUsed");
	}

	/** "Blaze: 112 / 256" */
	public Component getHorde() {
		EntityType<?> type = getMonsterType();
		Component name = type == null ? Component.empty() : type.getDescription();
		return armed > 0 ? tr("horde_armed", name, count, getCapacity(), armed) : tr("horde", name, count, getCapacity());
	}

	/** The Netherite cage's field: up, down, counting down to an escape, or the clock to the next one. */
	public Component getContainment() {
		if (fieldUp)
			return tr("field_up");
		return escapeIn < 0 ? tr("field_down")
			: paused ? tr("alarm_paused", time(escapeIn))
			: isWarning() ? tr("alarm", time(escapeIn)) : tr("next_escape", time(escapeIn));
	}

	public Component getStatus() {
		if (!hasMonster())
			return tr("empty");
		if (isNetherite() && !fieldUp)
			return getContainment();
		if (isDrowning())
			return water.getFluidAmount() > 0 ? tr("drowning", Math.round(100f * drownTicks / DROWN_TICKS)) : tr("needs_water");
		if (getSpeed() == 0 || isOverStressed())
			return tr("idle");
		if (peaceful())
			return tr("peaceful_status");
		if (count >= getCapacity())
			return tr("full");
		if (getChowCount() < chowCost())
			return tr("hungry", chowCost());
		if (isFlooded() && water.getFluidAmount() < waterPerMonster())
			return tr("needs_water");
		if (isNetherite() && !farmingPowered)
			return tr("short_of_fe");
		return tr("growing", Math.round(100 * getProgress()));
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		lang().translate("gui.goggles.monster_cage").forGoggles(tooltip);
		if (!hasMonster()) {
			lang().add(tr("empty")).style(ChatFormatting.RED).forGoggles(tooltip, 1);
		} else {
			lang().add(getHorde().copy()).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
			lang().add(getStatus().copy()).style(isGrowing() ? ChatFormatting.AQUA : ChatFormatting.RED).forGoggles(tooltip, 1);
			float seconds = secondsPerMonster();
			if (seconds > 0)
				lang().add(tr("per_monster", chowCost(), String.format(Locale.ROOT, "%.1f", seconds))).style(ChatFormatting.GRAY)
					.forGoggles(tooltip, 1);
			int monstrous = getMonstrousLevel();
			if (monstrous > 0)
				lang().add(tr("monstrous", Component.translatable("enchantment.level." + monstrous)))
					.style(ChatFormatting.LIGHT_PURPLE).forGoggles(tooltip, 1);
		}
		if (isFlooded())
			lang().text(String.format(Locale.ROOT, "%,d / %,d mB ", water.getFluidAmount(), water.getCapacity()))
				.style(ChatFormatting.GRAY).add(lang().add(new FluidStack(Fluids.WATER, 1).getHoverName())).forGoggles(tooltip, 1);
		if (isNetherite())
			lang().add(tr("energy", String.format(Locale.ROOT, "%,d", energy.getEnergyStored()),
				String.format(Locale.ROOT, "%,d", energy.getMaxEnergyStored()))).style(fieldUp ? ChatFormatting.AQUA : ChatFormatting.RED)
				.forGoggles(tooltip, 1);
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		return true;
	}

	private static MutableComponent tr(String key, Object... args) {
		return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.monster_cage." + key, args);
	}

	private static LangBuilder lang() {
		return new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID);
	}

	// ---------------------------------------------------------------- output and dropping

	public IItemHandler getAutomationHandler() {
		return automation;
	}

	public boolean hasOutput() {
		if (showsSummary())
			return trayUsed > 0;
		for (int i = 0; i < output.getSlots(); i++)
			if (!output.getStackInSlot(i).isEmpty())
				return true;
		return false;
	}

	void giveOutputTo(Player player) {
		for (int i = 0; i < output.getSlots(); i++) {
			ItemStack stack = output.extractItem(i, Integer.MAX_VALUE, false);
			if (!stack.isEmpty())
				player.getInventory().placeItemBackInInventory(stack);
		}
	}

	/** Broken: the jar, the chow and the output, and the rest of the horde culled into drops (D29). */
	void dropContents() {
		if (!(level instanceof ServerLevel server))
			return;
		List<ItemStack> all = new ArrayList<>();
		if (hasMonster())
			all.addAll(cull(server, count - 1, null, ItemStack.EMPTY));
		all.add(captive);
		for (int i = 0; i < chow.getSlots(); i++)
			all.add(chow.getStackInSlot(i));
		for (int i = 0; i < output.getSlots(); i++)
			all.add(output.getStackInSlot(i));
		for (ItemStack stack : all)
			if (!stack.isEmpty())
				Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
		clearContent();
	}

	int getComparatorOutput() {
		if (isWarning())
			return 15;
		return hasMonster() ? Mth.clamp(Math.round(15f * count / getCapacity()), 1, 15) : 0;
	}

	private void onInventoryChanged() {
		setChanged();
		syncQueued = true;
		// a client's inventories change only through an open menu (or in Ponder): the bowl and tray follow them
		if (showsSummary())
			summarise();
	}

	@Override
	public void clearContent() {
		holdCaptive(ItemStack.EMPTY);
		count = 0;
		armed = 0;
		sample = null;
		for (int i = 0; i < chow.getSlots(); i++)
			chow.setStackInSlot(i, ItemStack.EMPTY);
		for (int i = 0; i < output.getSlots(); i++)
			output.setStackInSlot(i, ItemStack.EMPTY);
		water.setFluid(FluidStack.EMPTY);
	}

	// ---------------------------------------------------------------- the item's enchantments (Monstrous)

	@Override
	protected void applyImplicitComponents(DataComponentInput input) {
		super.applyImplicitComponents(input);
		enchantments = input.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (!enchantments.isEmpty())
			components.set(DataComponents.ENCHANTMENTS, enchantments);
	}

	@Override
	public void removeComponentsFromTag(CompoundTag tag) {
		super.removeComponentsFromTag(tag);
		tag.remove("Enchantments");
	}

	// ---------------------------------------------------------------- persistence

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		if (clientPacket && omitCaptive)
			tag.putBoolean("SameCaptive", true);
		else if (!captive.isEmpty())
			tag.put("Captive", captive.save(registries));
		tag.putInt("Count", count);
		tag.putInt("Armed", armed);
		tag.putFloat("Progress", progress);
		if (clientPacket) {
			writeSummary(tag, registries);
			// the impact the network holds, settled, so the goggles agree with it
			if (!Float.isNaN(impact.applied()))
				tag.putFloat("Impact", impact.applied());
		} else if (!showsSummary()) {
			// a client's inventories aren't the cage's (a creative pick-block copy mustn't paste a stale bowl and tray)
			tag.put("Chow", chow.serializeNBT(registries));
			tag.put("Output", output.serializeNBT(registries));
		}
		if (isFlooded())
			tag.put("Water", water.writeToNBT(registries, new CompoundTag()));
		if (isNetherite()) {
			tag.putInt("Energy", energy.getEnergyStored());
			tag.putBoolean("FieldUp", fieldUp);
			tag.putBoolean("FarmingPowered", farmingPowered);
			tag.putInt("EscapeIn", escapeIn);
			tag.putBoolean("Paused", paused);
			if (owner != null)
				tag.putUUID("Owner", owner);
			tag.putString("OwnerName", ownerName);
		}
		tag.putInt("DrownTicks", drownTicks);
		if (!enchantments.isEmpty())
			ItemEnchantments.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), enchantments)
				.ifSuccess(encoded -> tag.put("Enchantments", encoded));
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		// a sync that left the captive out (see getUpdatePacket): it's the one last sent, whatever a click here guessed;
		// and the same jar again keeps the stack it has, so what's cached against it (the jar's drawn monster) stays
		if (clientPacket && tag.getBoolean("SameCaptive")) {
			if (receivedCaptive != null && captive != receivedCaptive) {
				holdCaptive(receivedCaptive);
				sample = null;
				shown.clear();
			}
		} else {
			ItemStack read = ItemStack.parseOptional(registries, tag.getCompound("Captive"));
			if (!ItemStack.isSameItemSameComponents(captive, read)) {
				holdCaptive(read);
				sample = null;
				shown.clear();
			}
			if (clientPacket)
				receivedCaptive = captive;
		}
		if (clientPacket)
			syncedImpact = tag.contains("Impact") ? tag.getFloat("Impact") : Float.NaN;
		count = tag.getInt("Count");
		int wasArmed = armed;
		armed = tag.getInt("Armed");
		if (clientPacket && armed != wasArmed)
			shown.clear();
		progress = tag.getFloat("Progress");
		if (clientPacket) {
			readSummary(tag, registries);
		} else {
			chow.deserializeNBT(registries, tag.getCompound("Chow"));
			output.deserializeNBT(registries, tag.getCompound("Output"));
			summarise();
		}
		if (tag.contains("Water"))
			water.readFromNBT(registries, tag.getCompound("Water"));
		energy.set(tag.getInt("Energy"));
		fieldUp = tag.getBoolean("FieldUp");
		farmingPowered = !tag.contains("FarmingPowered") || tag.getBoolean("FarmingPowered");
		escapeIn = tag.contains("EscapeIn") ? tag.getInt("EscapeIn") : -1;
		paused = tag.getBoolean("Paused");
		owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
		ownerName = tag.getString("OwnerName");
		drownTicks = tag.getInt("DrownTicks");
		enchantments = tag.contains("Enchantments")
			? ItemEnchantments.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("Enchantments"))
				.result().orElse(ItemEnchantments.EMPTY)
			: ItemEnchantments.EMPTY;
	}

	/** At a sync: the jar goes with it unless an earlier one sent it, and again once that was a while ago. */
	private void markCaptiveSent(long now) {
		if (captive != sentCaptive || now - sentCaptiveAt >= CAPTIVE_RESEND_TICKS) {
			sentCaptive = captive;
			sentCaptiveAt = now;
		}
	}

	/**
	 * The changes this cage's own syncs send leave out the captive when it's the one clients were last sent (its jar
	 * carries the whole monster). A client first seeing the cage gets everything, from {@code getUpdateTag}.
	 */
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this, (be, registries) -> ((MonsterCageBlockEntity) be).updatePacketTag(registries));
	}

	private CompoundTag updatePacketTag(HolderLookup.Provider registries) {
		long now = level == null ? 0 : level.getGameTime();
		// its own sync is broadcast at the start of the next tick; the jar is left out only if an earlier one of its own
		// syncs sent it (building a packet doesn't count: only sendData, which broadcasts to every client watching)
		omitCaptive = now - ownSyncAt <= 1 && captive == sentCaptive && sentCaptiveAt < ownSyncAt;
		try {
			return writeClient(new CompoundTag(), registries);
		} finally {
			omitCaptive = false;
		}
	}

	// ---------------------------------------------------------------- handlers

	/** The chow bin: normal stacks of Monster Chow; putting some in rattles it into the bowl. */
	private class ChowHandler extends ItemStackHandler {
		ChowHandler(int slots) {
			super(slots);
		}

		@Override
		public int getSlotLimit(int slot) {
			return 64;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return isChow(stack);
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			ItemStack left = super.insertItem(slot, stack, simulate);
			if (!simulate && left.getCount() < stack.getCount() && level != null && !level.isClientSide
				&& level.getGameTime() - lastPour > 20) {
				lastPour = level.getGameTime();
				level.playSound(null, worldPosition, ModSounds.cage("chow_pour"), SoundSource.BLOCKS, 1, 1);
			}
			return left;
		}

		@Override
		protected void onContentsChanged(int slot) {
			onInventoryChanged();
		}
	}

	/** The screen's jar slot: put a jar in an empty cage, or take the monster back out (not mid-alarm). */
	private class CaptiveHandler implements IItemHandlerModifiable {
		@Override
		public int getSlots() {
			return 1;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return captive.copy();
		}

		@Override
		public void setStackInSlot(int slot, ItemStack stack) {
			if (!stack.isEmpty() && hasMonster() && ItemStack.isSameItemSameComponents(stack, captive))
				return;
			if (level == null || level.isClientSide) {
				holdCaptive(stack.copyWithCount(Math.min(1, stack.getCount())));
				count = captive.isEmpty() ? 0 : Math.max(1, count);
				sample = null;
				shown.clear();
				return;
			}
			if (isLockedDown())
				return;
			if (stack.isEmpty()) {
				release();
				return;
			}
			if (hasMonster())
				release();
			setCaptive(stack.copyWithCount(1));
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (hasMonster() || !isItemValid(slot, stack))
				return stack;
			if (!simulate)
				setStackInSlot(slot, stack);
			return stack.copyWithCount(stack.getCount() - 1);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			if (amount <= 0 || !hasMonster() || isLockedDown())
				return ItemStack.EMPTY;
			ItemStack out = captive.copy();
			if (!simulate)
				setStackInSlot(slot, ItemStack.EMPTY);
			return out;
		}

		@Override
		public int getSlotLimit(int slot) {
			return 1;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			EntityType<?> type = MonsterJarItem.typeOf(stack);
			return MonsterJarItem.isFilled(stack) && type != null && CageRules.fits(getTier(), isFlooded(), type);
		}
	}

	/** Automation: chow in from any side, the drops out. */
	private class AutomationHandler implements IItemHandler {
		@Override
		public int getSlots() {
			return chow.getSlots() + output.getSlots();
		}

		private boolean isBin(int slot) {
			return slot < chow.getSlots();
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return isBin(slot) ? chow.getStackInSlot(slot) : output.getStackInSlot(slot - chow.getSlots());
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			return isBin(slot) ? chow.insertItem(slot, stack, simulate) : stack;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return isBin(slot) ? ItemStack.EMPTY : output.extractItem(slot - chow.getSlots(), amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			return 64;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return isBin(slot) && isChow(stack);
		}
	}

	/** The Netherite cage's store: FE in from any side but the back, nothing out. */
	private class CageEnergy extends EnergyStorage {
		CageEnergy(int capacity) {
			super(capacity, capacity, 0);
		}

		void take(int amount) {
			energy = Math.max(0, energy - amount);
			energyChanged = true;
		}

		void set(int amount) {
			energy = Mth.clamp(amount, 0, capacity);
		}

		@Override
		public int receiveEnergy(int toReceive, boolean simulate) {
			if (capacity <= 0)
				return 0;
			if (!takesFe()) {
				// off, it takes nothing: the FE stays on the network for other machines, and what was offered is noted
				if (toReceive > 0)
					offeredNow += toReceive;
				return 0;
			}
			// on trial, real FE comes in at most at the field's cost a tick; a simulation still answers with the room (a
			// Power Line plans a second ahead with one), and what a pusher can't put in it keeps
			if (startingFor >= 0 && !simulate)
				toReceive = (int) Math.min(toReceive, Math.max(0, CMConfig.Server.get(CMConfig.SERVER.cageFieldFe) - receivedNow));
			int got = super.receiveEnergy(toReceive, simulate);
			if (got > 0 && !simulate) {
				receivedNow += got;
				// marked at once: a cable can feed a cage whose own chunk isn't ticking, and it would be lost on unload
				if (level != null)
					level.blockEntityChanged(worldPosition);
			}
			return got;
		}
	}
}
