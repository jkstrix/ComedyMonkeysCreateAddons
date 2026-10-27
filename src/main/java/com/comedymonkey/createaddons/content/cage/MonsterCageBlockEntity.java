package com.comedymonkey.createaddons.content.cage;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.FixedStress;
import com.comedymonkey.createaddons.content.MachineEnchantments;
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
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
	private final CageEnergy energy;
	private final IItemHandler automation = new AutomationHandler();
	private final IItemHandlerModifiable captiveSlot = new CaptiveHandler();
	private boolean syncQueued;
	@Nullable
	private Entity sample;

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
		energy = new CageEnergy(tier == CageTier.NETHERITE ? CMConfig.Server.get(CMConfig.SERVER.cageFeBuffer) : 0);
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
		return MonsterJarItem.typeOf(captive);
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
		int n = 0;
		for (int i = 0; i < chow.getSlots(); i++)
			n += chow.getStackInSlot(i).getCount();
		return n;
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

	private void setCaptive(ItemStack jar) {
		captive = jar;
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
					setCaptive(stack.split(1));
					level.playSound(null, worldPosition, ModSounds.cage("jar_in"), SoundSource.BLOCKS, 1, 1);
					if (!player.getAbilities().instabuild || player instanceof FakePlayer)
						player.getInventory().placeItemBackInInventory(((MonsterJarItem) captive.getItem()).empty());
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
		return drops;
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
		if (FixedStress.keep(this, wantedImpact()))
			sendData();
		if (isNetherite())
			field();
		if (isFlooded())
			flooded();
		if (canGrow())
			grow();
		grumble();
		if (syncQueued && level.getGameTime() % SYNC_INTERVAL == 0) {
			syncQueued = false;
			sendData();
		}
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
		float impact = wantedImpact();
		lastStressApplied = impact;
		return impact;
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
		if (level != null && !level.isClientSide && FixedStress.keep(this, wantedImpact()))
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
		captive = jar;
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
		if (target == null)
			return;
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

	public IFluidHandler getWaterHandler() {
		return water;
	}

	// ---------------------------------------------------------------- the Netherite cage's field

	private int warningTicks() {
		return 20 * CMConfig.Server.get(CMConfig.SERVER.cageWarningSeconds);
	}

	/** Takes the field's FE, and runs the countdown and the escapes while it's down. */
	private void field() {
		if (!hasMonster()) {
			fieldUp = false;
			escapeIn = -1;
			return;
		}
		boolean was = fieldUp;
		int fieldCost = CMConfig.Server.get(CMConfig.SERVER.cageFieldFe);
		int farmCost = getSpeed() == 0 || count >= getCapacity() ? 0
			: Math.round(CMConfig.Server.get(CMConfig.SERVER.cageFarmFe) * Math.min(1, Math.abs(getSpeed()) / 256f));
		fieldUp = energy.getEnergyStored() >= fieldCost;
		if (fieldUp) {
			energy.take(fieldCost);
			farmingPowered = energy.getEnergyStored() >= farmCost;
			if (farmingPowered)
				energy.take(farmCost);
		}
		if (fieldUp != was) {
			level.playSound(null, worldPosition, ModSounds.cage(fieldUp ? "field_up" : "field_down"), SoundSource.BLOCKS, 1, 1);
			sendData();
		}
		if (fieldUp) {
			if (escapeIn >= 0) {
				if (escapeIn <= warningTicks())
					announce("alarm_restored");
				escapeIn = -1;
				paused = false;
				sendData();
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
		int want = hasMonster() ? Math.min(count, isNetherite() ? 1 : SHOWN) : 0;
		while (shown.size() > want)
			shown.removeLast();
		while (shown.size() < want) {
			Entity made = MonsterJarItem.create(captive, level).orElse(null);
			if (made == null)
				break;
			if (made instanceof Mob mob)
				for (var slot : net.minecraft.world.entity.EquipmentSlot.values())
					mob.setItemSlot(slot, ItemStack.EMPTY);
			// an armed drowned holds its trident up
			if (made instanceof Mob mob && shown.size() < armed && getMonsterType() == EntityType.DROWNED)
				mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
			shown.add(new CageMonster(made, level.random, isNetherite()));
		}
		for (CageMonster monster : shown)
			monster.tick(level.random, isRunning() || getSpeed() != 0);
		clientTicker.accept(this);
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
		for (int i = 0; i < chow.getSlots(); i++)
			if (!chow.getStackInSlot(i).isEmpty())
				return chow.getStackInSlot(i);
		return ItemStack.EMPTY;
	}

	/** "Blaze: 112 / 256" */
	public Component getHorde() {
		EntityType<?> type = getMonsterType();
		Component name = type == null ? Component.empty() : type.getDescription();
		return armed > 0 ? tr("horde_armed", name, count, getCapacity(), armed) : tr("horde", name, count, getCapacity());
	}

	public Component getStatus() {
		if (!hasMonster())
			return tr("empty");
		if (isNetherite() && !fieldUp)
			return escapeIn < 0 ? tr("field_down")
				: paused ? tr("alarm_paused", time(escapeIn))
				: isWarning() ? tr("alarm", time(escapeIn)) : tr("next_escape", time(escapeIn));
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
	}

	@Override
	public void clearContent() {
		captive = ItemStack.EMPTY;
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
		if (!captive.isEmpty())
			tag.put("Captive", captive.save(registries));
		tag.putInt("Count", count);
		tag.putInt("Armed", armed);
		tag.putFloat("Progress", progress);
		tag.put("Chow", chow.serializeNBT(registries));
		tag.put("Output", output.serializeNBT(registries));
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
		ItemStack was = captive;
		captive = ItemStack.parseOptional(registries, tag.getCompound("Captive"));
		if (!ItemStack.isSameItemSameComponents(was, captive)) {
			sample = null;
			shown.clear();
		}
		count = tag.getInt("Count");
		int wasArmed = armed;
		armed = tag.getInt("Armed");
		if (clientPacket && armed != wasArmed)
			shown.clear();
		progress = tag.getFloat("Progress");
		chow.deserializeNBT(registries, tag.getCompound("Chow"));
		output.deserializeNBT(registries, tag.getCompound("Output"));
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
				captive = stack.copyWithCount(Math.min(1, stack.getCount()));
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
			onInventoryChanged();
		}

		void set(int amount) {
			energy = Mth.clamp(amount, 0, capacity);
		}

		@Override
		public int receiveEnergy(int toReceive, boolean simulate) {
			int got = super.receiveEnergy(toReceive, simulate);
			if (got > 0 && !simulate)
				onInventoryChanged();
			return got;
		}
	}
}
