package com.comedymonkey.createaddons.content.tank;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
import com.comedymonkey.createaddons.content.pen.BottledAnimalItem;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A Fish Tank (design/specs/monster-cage-and-fish-tank.md, Part 2). It keeps one bottled water creature and a school of
 * it, breeding as the Animal Pens do: every interval each adult eats one Fish Food, uses 250 mB of water and has one
 * young (F5, F11). The interval is 5 minutes at 64 RPM and scales with speed and Fruitful. Fish and squid have no young
 * in vanilla, so theirs arrive grown; axolotl young grow up. Young copy their parent; an axolotl birth is blue one time
 * in 1,200, as in vanilla (F13). A sword culls half the adults for their drops; a water bucket scoops one, blue first
 * (F6). A tank of glow squid gives light (F15).
 */
public class FishTankBlockEntity extends KineticBlockEntity implements Clearable, MenuProvider {
	public static final int SHOWN = 4;
	private static final int SYNC_INTERVAL = 10;
	private static final int BLUE_ODDS = 1200;

	public static Consumer<FishTankBlockEntity> clientTicker = be -> {};

	private ItemStack captive = ItemStack.EMPTY;
	/** The captive's creature, read once whenever the captive changes. */
	@Nullable
	private EntityType<?> creatureType;
	private ItemEnchantments enchantments = ItemEnchantments.EMPTY;
	private int adults;
	/** Blue axolotls among the adults. */
	private int blue;
	private final List<int[]> young = new ArrayList<>();
	private float progress;
	/** The captive's bottle made no creature (its mod gone, a tag changed): not tried again until it changes. */
	private boolean createFailed;
	/** Client: the colour of the school's axolotls that aren't blue. */
	@Nullable
	private Axolotl.Variant ownVariant;
	/** The young's growth this tick short of a whole tick, carried to the next. */
	private float growCarry;
	private final ItemStackHandler food;
	private final ItemStackHandler output;
	private final FluidTank water;
	/** What pipes see: water goes in, nothing comes out. */
	private final IFluidHandler waterIn;
	private final IItemHandler automation = new AutomationHandler();
	private final IItemHandlerModifiable captiveSlot = new CaptiveHandler();
	private boolean syncQueued;
	/** Queued syncs go out on this tick of every {@link #SYNC_INTERVAL}, by position, so a wall of tanks doesn't sync all at once. */
	private final int syncPhase;
	/** Client: the trough and the net as the server summed them up (clients aren't sent the inventories; an open menu syncs its own slots). */
	private ItemStack shownFood = ItemStack.EMPTY;
	private int shownFoodCount, trayUsed;
	private final List<ItemStack> tray = new ArrayList<>(TRAY_SHOWN);
	private static final int TRAY_SHOWN = 3;
	/** The stress impact the network holds: a rise at once, a fall once it has lasted (see {@link SettlingImpact}). */
	private final SettlingImpact impact = new SettlingImpact();
	@Nullable
	private AABB renderBox;
	/** Server: the captive as last sent to clients, when, and this tank's own last sync (as the pens'). */
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
	private long lastFeed = Long.MIN_VALUE;
	@Nullable
	private Entity sample;
	private final List<TankCreature> shown = new ArrayList<>();
	/** Client: the camera's distance this tick (for the window and the loops); endless where it can't be told. */
	private double viewDistanceSqr;

	public FishTankBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FISH_TANK.get(), pos, state);
		boolean brass = isBrass(state);
		food = new ItemStackHandler(brass ? 8 : 2) {
			@Override
			public int getSlotLimit(int slot) {
				return 64;
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return stack.is(ModItems.FISH_FOOD.get());
			}

			@Override
			public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
				ItemStack left = super.insertItem(slot, stack, simulate);
				if (!simulate && left.getCount() < stack.getCount() && level != null && !level.isClientSide
					&& level.getGameTime() - lastFeed > 20) {
					lastFeed = level.getGameTime();
					level.playSound(null, worldPosition, ModSounds.tank("feeder_sprinkle"), SoundSource.BLOCKS, 1, 1);
				}
				return left;
			}

			@Override
			protected void onContentsChanged(int slot) {
				onInventoryChanged();
			}
		};
		output = new ItemStackHandler(brass ? 18 : 9) {
			@Override
			protected void onContentsChanged(int slot) {
				onInventoryChanged();
			}
		};
		water = new FluidTank(CMConfig.Server.get(brass ? CMConfig.SERVER.brassFishTankWater : CMConfig.SERVER.fishTankWater),
			stack -> stack.getFluid().isSame(Fluids.WATER)) {
			@Override
			protected void onContentsChanged() {
				onInventoryChanged();
			}
		};
		waterIn = new FillOnly(water);
		syncPhase = SyncStagger.phase(pos, SYNC_INTERVAL);
	}

	private static boolean isBrass(BlockState state) {
		return state.getBlock() instanceof FishTankBlock tank && tank.isBrass();
	}

	public boolean isBrass() {
		return isBrass(getBlockState());
	}

	public int getCapacity() {
		return CMConfig.Server.get(isBrass() ? CMConfig.SERVER.brassFishTankCapacity : CMConfig.SERVER.fishTankCapacity);
	}

	public int getFruitfulLevel() {
		return MachineEnchantments.levelIn(enchantments, MachineEnchantments.FRUITFUL);
	}

	private float speedUp() {
		return MachineEnchantments.speedMultiplier(getFruitfulLevel(), CMConfig.SERVER.penFruitfulBonus);
	}

	/** Ticks per breeding at 64 RPM, sped up by Fruitful. */
	private float breedTime() {
		return Math.max(1, 20f * CMConfig.Server.get(CMConfig.SERVER.fishTankBreedSeconds) / speedUp());
	}

	private int growTime() {
		return Math.max(1, Math.round(20f * CMConfig.Server.get(CMConfig.SERVER.penGrowSeconds) / speedUp()));
	}

	private int waterPerBirth() {
		return CMConfig.Server.get(CMConfig.SERVER.fishTankWaterPerBirth);
	}

	// ---------------------------------------------------------------- the creature

	public boolean hasCreature() {
		return !captive.isEmpty() && getCount() > 0;
	}

	public int getCount() {
		int n = adults;
		for (int[] litter : young)
			n += litter[0];
		return n;
	}

	public int getAdults() {
		return adults;
	}

	public int getYoung() {
		return getCount() - adults;
	}

	public int getBlue() {
		return blue;
	}

	public ItemStack getCaptive() {
		return captive;
	}

	/** Every change of the captive comes through here, so its creature's type is read once per bottle. */
	private void holdCaptive(ItemStack bottle) {
		captive = bottle;
		creatureType = BottledAnimalItem.typeOf(bottle);
	}

	@Nullable
	public Entity sample() {
		if (sample == null && !createFailed && hasCreature() && level != null) {
			sample = BottledAnimalItem.create(captive, level).orElse(null);
			createFailed = sample == null;
			if (sample instanceof AgeableMob grown)
				grown.setAge(0);
			if (sample != null)
				sample.setPos(Vec3.atCenterOf(worldPosition));
		}
		return sample;
	}

	public boolean isAxolotl() {
		return creatureType == EntityType.AXOLOTL;
	}

	public int getFoodCount() {
		if (showsSummary())
			return shownFoodCount;
		int n = 0;
		for (int i = 0; i < food.getSlots(); i++)
			n += food.getStackInSlot(i).getCount();
		return n;
	}

	/** Clients go by the server's summary of the trough and the net (see {@link #writeSummary}), not the inventories. */
	boolean showsSummary() {
		return level != null && level.isClientSide;
	}

	public ItemStack getShownFood() {
		if (showsSummary())
			return shownFood;
		for (int i = 0; i < food.getSlots(); i++)
			if (!food.getStackInSlot(i).isEmpty())
				return food.getStackInSlot(i);
		return ItemStack.EMPTY;
	}

	private void eat(int amount) {
		for (int i = 0; i < food.getSlots() && amount > 0; i++)
			amount -= food.extractItem(i, amount, false).getCount();
	}

	/** Ponder and game tests: this creature, with so many adults (blue ones among them) and young. */
	public void setPonderSchool(ItemStack bottle, int grown, int babies, int blueOnes) {
		setCaptive(bottle.copyWithCount(1));
		adults = grown;
		blue = Math.min(blueOnes, grown);
		young.clear();
		if (babies > 0)
			young.add(new int[] {babies, growTime(), 0});
		setChanged();
		sendData();
	}

	/** Ponder only: the Fish Food in the trough and the water in the tank. */
	public void setPonderSupplies(ItemStack fishFood, int waterMb) {
		for (int i = 0; i < food.getSlots(); i++)
			food.setStackInSlot(i, i == 0 ? fishFood.copy() : ItemStack.EMPTY);
		water.setFluid(waterMb > 0 ? new FluidStack(Fluids.WATER, Math.min(waterMb, water.getCapacity())) : FluidStack.EMPTY);
	}

	private void setCaptive(ItemStack bottle) {
		holdCaptive(bottle);
		adults = bottle.isEmpty() ? 0 : 1;
		blue = 0;
		young.clear();
		if (!bottle.isEmpty() && bottle.get(DataComponents.ENTITY_DATA) != null) {
			CompoundTag tag = bottle.get(DataComponents.ENTITY_DATA).copyTag();
			int isBlue = creatureType == EntityType.AXOLOTL && tag.getInt("Variant") == Axolotl.Variant.BLUE.getId() ? 1 : 0;
			if (tag.getInt("Age") < 0) {
				adults = 0;
				young.add(new int[] {1, Math.min(-tag.getInt("Age"), growTime()), isBlue});
			} else {
				blue = isBlue;
			}
		}
		progress = 0;
		sample = null;
		createFailed = false;
		shown.clear();
		updateGlow();
		setChanged();
		sendData();
		if (level != null)
			level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
	}

	/** A tank of glow squid glows (F15). */
	private void updateGlow() {
		if (level == null || level.isClientSide)
			return;
		boolean glow = hasCreature() && creatureType == EntityType.GLOW_SQUID;
		BlockState state = getBlockState();
		if (state.hasProperty(FishTankBlock.GLOWING) && state.getValue(FishTankBlock.GLOWING) != glow)
			level.setBlock(worldPosition, state.setValue(FishTankBlock.GLOWING, glow), 3);
	}

	private ItemStack release() {
		if (!hasCreature())
			return ItemStack.EMPTY;
		if (level instanceof ServerLevel server)
			store(cull(server, Math.max(0, adults - 1), null, ItemStack.EMPTY));
		ItemStack out = captive;
		setCaptive(ItemStack.EMPTY);
		return out;
	}

	// ---------------------------------------------------------------- using it

	boolean use(Player player, InteractionHand hand, ItemStack stack) {
		boolean client = level == null || level.isClientSide;
		// a water bucket scoops a creature that fits in one, else it fills the tank (never the world beside it)
		if (stack.is(Items.WATER_BUCKET)) {
			if (!client) {
				if (hasCreature() && sample() instanceof Bucketable)
					scoop(player, hand, stack);
				else
					pourIn(player, hand, stack);
			}
			return true;
		}
		if (!hasCreature()) {
			if (!(stack.getItem() instanceof BottledAnimalItem))
				return false;
			// the bottle goes in with its creature and comes back out with it, as in the pens
			if (!client) {
				if (TankRules.livesInTank(stack, level))
					setCaptive(stack.split(1));
				else
					player.displayClientMessage(tr("not_tank_creature"), true);
			}
			return true;
		}
		if (stack.is(ItemTags.SWORDS)) {
			if (!client)
				swipe(player, hand, stack);
			return true;
		}
		if (stack.is(ModItems.FISH_FOOD.get())) {
			if (!client) {
				ItemStack left = ItemHandlerHelper.insertItemStacked(food, stack.copy(), false);
				if (!player.getAbilities().instabuild)
					stack.setCount(left.getCount());
			}
			return true;
		}
		return false;
	}

	/** A water bucket emptied into the tank, if there's room for all of it. */
	private void pourIn(Player player, InteractionHand hand, ItemStack bucket) {
		FluidStack bucketful = new FluidStack(Fluids.WATER, FluidType.BUCKET_VOLUME);
		if (water.fill(bucketful, IFluidHandler.FluidAction.SIMULATE) < FluidType.BUCKET_VOLUME)
			return;
		water.fill(bucketful, IFluidHandler.FluidAction.EXECUTE);
		player.setItemInHand(hand, ItemUtils.createFilledResult(bucket, player, new ItemStack(Items.BUCKET)));
		level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1, 1);
	}

	private void swipe(Player player, InteractionHand hand, ItemStack sword) {
		// a creature whose mod has gone can't be culled
		if (!(level instanceof ServerLevel server) || !(sample() instanceof LivingEntity))
			return;
		int killed = Math.min(Math.round(adults / 2f), adults - 1);
		if (killed <= 0) {
			player.displayClientMessage(tr(adults == 0 ? "too_young" : "last_one"), true);
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
		setChanged();
		sendData();
		server.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
	}

	/** One live adult into the bucket, a blue axolotl first; never the last (F6, F13, F18). */
	private void scoop(Player player, InteractionHand hand, ItemStack bucket) {
		if (adults <= 1) {
			player.displayClientMessage(tr("last_one"), true);
			return;
		}
		Entity made = BottledAnimalItem.create(captive, level).orElse(null);
		if (!(made instanceof Bucketable bucketable))
			return;
		if (made instanceof AgeableMob mob)
			mob.setAge(0);
		boolean isBlue = made instanceof Axolotl && blue > 0;
		if (made instanceof Axolotl axolotl && isBlue)
			axolotl.setVariant(Axolotl.Variant.BLUE);
		ItemStack filled = bucketable.getBucketItemStack();
		bucketable.saveToBucketTag(filled);
		adults--;
		if (isBlue)
			blue--;
		player.setItemInHand(hand, ItemUtils.createFilledResult(bucket, player, filled));
		level.playSound(null, worldPosition, bucketable.getPickupSound(), SoundSource.PLAYERS, 1, 1);
		setChanged();
		sendData();
		level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
	}

	private List<ItemStack> cull(ServerLevel server, int killed, @Nullable Player killer, ItemStack weapon) {
		List<ItemStack> drops = new ArrayList<>();
		if (killed <= 0 || !(sample() instanceof LivingEntity creature))
			return drops;
		LootTable table = server.getServer().reloadableRegistries().getLootTable(creature.getLootTable());
		DamageSource source = killer != null ? server.damageSources().playerAttack(killer) : server.damageSources().generic();
		boolean burning = !weapon.isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(
			server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FIRE_ASPECT), weapon) > 0;
		if (burning)
			creature.igniteForSeconds(4);
		for (int i = 0; i < killed; i++) {
			LootParams.Builder params = new LootParams.Builder(server)
				.withParameter(LootContextParams.THIS_ENTITY, creature)
				.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
				.withParameter(LootContextParams.DAMAGE_SOURCE, source);
			if (killer != null)
				params.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer)
					.withParameter(LootContextParams.ATTACKING_ENTITY, killer)
					.withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, killer)
					.withLuck(killer.getLuck());
			drops.addAll(table.getRandomItems(params.create(LootContextParamSets.ENTITY)));
		}
		if (burning)
			creature.clearFire();
		// the blue ones go last
		int notBlue = adults - blue;
		adults -= killed;
		if (killed > notBlue)
			blue -= killed - notBlue;
		// a big school's cull is many rolls: joined into full stacks before they're stored or dropped
		return Drops.merged(drops);
	}

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
		if (any && level != null)
			level.playSound(null, worldPosition, SoundEvents.FISH_SWIM, SoundSource.BLOCKS, 0.5f, 1.2f);
	}

	private Vec3 frontOf(double out) {
		Direction front = getBlockState().getValue(FishTankBlock.FACING);
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
		if (isRunning())
			work();
		// sendData clears the queue, so a sync this tick already sent leaves nothing to send again
		if (syncQueued && SyncStagger.due(level.getGameTime(), syncPhase, SYNC_INTERVAL))
			sendData();
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		updateGlow();
	}

	public boolean isRunning() {
		return getSpeed() != 0 && !isOverStressed() && hasCreature();
	}

	private boolean canBreed() {
		// a creature whose mod has gone doesn't breed (clients go by its type being known, rather than each tank in the
		// world making a creature to ask)
		return adults > 0 && getCount() < getCapacity() && getFoodCount() > 0 && water.getFluidAmount() >= waterPerBirth()
			&& (showsSummary() ? creatureType != null : sample() != null);
	}

	public boolean isBreeding() {
		return isRunning() && canBreed();
	}

	private void work() {
		float step = Math.abs(getSpeed()) / 64f;
		boolean grew = false;
		// the young grow by whole ticks, the fractions carried over, so they keep the breeding's pace at any speed
		growCarry += step;
		int ticks = (int) growCarry;
		growCarry -= ticks;
		for (int i = 0; ticks > 0 && i < young.size(); i++) {
			int[] litter = young.get(i);
			litter[1] -= ticks;
			if (litter[1] <= 0) {
				adults += litter[0];
				blue += litter[2];
				young.remove(i--);
				grew = true;
			}
		}
		if (grew)
			sendData();
		if (!canBreed())
			return;
		progress += step;
		if (progress < breedTime())
			return;
		progress = 0;
		int perBirth = waterPerBirth();
		int born = Math.min(Math.min(adults, getCapacity() - getCount()),
			Math.min(getFoodCount(), perBirth == 0 ? Integer.MAX_VALUE : water.getFluidAmount() / perBirth));
		if (born <= 0)
			return;
		eat(born);
		water.drain(born * perBirth, IFluidHandler.FluidAction.EXECUTE);
		if (isAxolotl()) {
			int blueBorn = 0;
			for (int i = 0; i < born; i++)
				if (level.random.nextInt(BLUE_ODDS) == 0)
					blueBorn++;
			young.add(new int[] {born, growTime(), blueBorn});
			if (blueBorn > 0)
				ModAdvancements.awardNearby(level, worldPosition, ModAdvancements.TANK_BLUE_AXOLOTL, ItemStack.EMPTY, blueBorn);
		} else {
			adults += born;
		}
		level.playSound(null, worldPosition, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.5f, 1.2f);
		setChanged();
		sendData();
		level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
	}

	public float getBreedProgress() {
		return Mth.clamp(progress / breedTime(), 0, 1);
	}

	/** Seconds per breeding at the current speed (0 when stopped). */
	public float secondsPerBreeding() {
		float speed = Math.abs(getSpeed());
		return speed == 0 ? 0 : breedTime() * 64 / speed / 20f;
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

	private float wantedImpact() {
		boolean working = hasCreature() && (canBreed() || !young.isEmpty());
		return working ? (float) CMConfig.Server.get(isBrass() ? CMConfig.SERVER.brassFishTankStress : CMConfig.SERVER.fishTankStress) : 0;
	}

	@Override
	public void onSpeedChanged(float previousSpeed) {
		super.onSpeedChanged(previousSpeed);
		if (level != null && !level.isClientSide && FixedStress.keep(this, impact()))
			sendData();
	}

	// ---------------------------------------------------------------- the window (client)

	public List<TankCreature> getShown() {
		return shown;
	}

	private void clientTick() {
		if (isBreeding())
			progress = Math.min(progress + Math.abs(getSpeed()) / 64f, breedTime() - 1);
		// the creatures in the water are only built near enough to be seen soon, and let go well out of sight
		viewDistanceSqr = ClientView.distanceSqr.applyAsDouble(this);
		int want = ClientView.keep(hasCreature() ? Math.min(getCount(), SHOWN) : 0, viewDistanceSqr);
		while (shown.size() > want)
			shown.removeLast();
		if (shown.isEmpty())
			ownVariant = null;
		while (shown.size() < want && !createFailed && ClientView.builds(viewDistanceSqr) && ClientView.mayBuild(level)) {
			Entity made = BottledAnimalItem.create(captive, level).orElse(null);
			if (made == null) {
				createFailed = true;
				break;
			}
			// the school's own colour: a caught blue axolotl's young are a common colour
			if (made instanceof Axolotl axolotl && ownVariant == null)
				ownVariant = axolotl.getVariant() == Axolotl.Variant.BLUE ? Axolotl.Variant.LUCY : axolotl.getVariant();
			shown.add(new TankCreature(made, level.random));
		}
		// young and blue ones by their place in the school, every tick, so a litter shows grown once it's grown (as the pens)
		int babies = getYoung() == 0 ? 0 : adults == 0 ? shown.size()
			: Mth.clamp(Math.round(shown.size() * getYoung() / (float) getCount()), 1, Math.max(1, shown.size() - 1));
		for (int i = 0; i < shown.size(); i++) {
			Entity entity = shown.get(i).entity();
			if (entity instanceof AgeableMob mob)
				mob.setBaby(i >= shown.size() - babies);
			if (entity instanceof Axolotl axolotl && ownVariant != null)
				axolotl.setVariant(i < blue ? Axolotl.Variant.BLUE : ownVariant);
		}
		float surface = waterLevel();
		for (TankCreature creature : shown)
			creature.tick(level.random, getSpeed() != 0, surface);
		clientTicker.accept(this);
	}

	/** Client: the camera's squared distance as of this tick (the loops go by it too). */
	public double getViewDistanceSqr() {
		return viewDistanceSqr;
	}

	/** The water's surface in model pixels. */
	public float waterLevel() {
		// from just over the gravel and the net (y 3.5 / 3.6), so the surface never lies on their tops and flickers
		return 3.7f + (13.5f - 3.7f) * Math.min(1, water.getFluidAmount() / (float) water.getCapacity());
	}

	// ---------------------------------------------------------------- screen and readouts

	@Override
	public Component getDisplayName() {
		return getBlockState().getBlock().getName();
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new FishTankMenu(containerId, inventory, this);
	}

	@Override
	public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
		buffer.writeBlockPos(worldPosition);
	}

	public IItemHandlerModifiable getCaptiveSlot() {
		return captiveSlot;
	}

	public IItemHandlerModifiable getFood() {
		return food;
	}

	public IItemHandlerModifiable getOutput() {
		return output;
	}

	public FluidTank getWater() {
		return water;
	}

	public IFluidHandler getWaterHandler() {
		return waterIn;
	}

	public IItemHandler getAutomationHandler() {
		return automation;
	}

	public Component getSchool() {
		EntityType<?> type = creatureType;
		Component name = type == null ? Component.empty() : type.getDescription();
		int babies = getYoung();
		return babies > 0 ? tr("school_young", name, adults, babies, getCapacity()) : tr("school", name, adults, getCapacity());
	}

	public Component getStatus() {
		if (!hasCreature())
			return tr("empty");
		if (getSpeed() == 0 || isOverStressed())
			return tr("idle");
		if (getCount() >= getCapacity())
			return tr("full");
		if (adults == 0)
			return tr("growing_up");
		if (getFoodCount() == 0)
			return tr("hungry");
		if (water.getFluidAmount() < waterPerBirth())
			return tr("needs_water");
		return tr("breeding", Math.round(100 * getBreedProgress()));
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		lang().translate("gui.goggles.fish_tank").forGoggles(tooltip);
		if (!hasCreature()) {
			lang().add(tr("empty")).style(ChatFormatting.RED).forGoggles(tooltip, 1);
		} else {
			lang().add(getSchool().copy()).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
			lang().add(getStatus().copy()).style(isBreeding() ? ChatFormatting.AQUA : ChatFormatting.RED).forGoggles(tooltip, 1);
			if (blue > 0)
				lang().add(tr("blue", blue)).style(ChatFormatting.BLUE).forGoggles(tooltip, 1);
			int fruitful = getFruitfulLevel();
			if (fruitful > 0)
				lang().add(Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.animal_pen.fruitful",
					Component.translatable("enchantment.level." + fruitful))).style(ChatFormatting.LIGHT_PURPLE).forGoggles(tooltip, 1);
		}
		lang().text(String.format(Locale.ROOT, "%,d / %,d mB ", water.getFluidAmount(), water.getCapacity()))
			.style(ChatFormatting.GRAY).add(lang().add(new FluidStack(Fluids.WATER, 1).getHoverName())).forGoggles(tooltip, 1);
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		return true;
	}

	private static MutableComponent tr(String key, Object... args) {
		return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.fish_tank." + key, args);
	}

	private static LangBuilder lang() {
		return new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID);
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

	void dropContents() {
		if (!(level instanceof ServerLevel server))
			return;
		List<ItemStack> all = new ArrayList<>();
		if (hasCreature())
			all.addAll(cull(server, Math.max(0, adults - 1), null, ItemStack.EMPTY));
		all.add(captive);
		for (int i = 0; i < food.getSlots(); i++)
			all.add(food.getStackInSlot(i));
		for (int i = 0; i < output.getSlots(); i++)
			all.add(output.getStackInSlot(i));
		for (ItemStack stack : all)
			if (!stack.isEmpty())
				Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
		clearContent();
	}

	int getComparatorOutput() {
		return hasCreature() ? Mth.clamp(Math.round(15f * getCount() / getCapacity()), 1, 15) : 0;
	}

	/** The first few stacks waiting in the net, for the tray. */
	public List<ItemStack> getTray() {
		return tray;
	}

	/** Output slots in use, for how full the net looks. */
	public int getTrayUsed() {
		return trayUsed;
	}

	/** The box the renderer draws in (the block's own), made once rather than every frame. */
	public AABB getRenderBox() {
		if (renderBox == null)
			renderBox = new AABB(worldPosition);
		return renderBox;
	}

	/** The trough and the net worked out again from the inventories (on the server, or where a client has them). */
	private void summarise() {
		shownFood = ItemStack.EMPTY;
		shownFoodCount = 0;
		for (int i = 0; i < food.getSlots(); i++) {
			ItemStack stack = food.getStackInSlot(i);
			if (shownFood.isEmpty() && !stack.isEmpty())
				shownFood = stack.copyWithCount(1);
			shownFoodCount += stack.getCount();
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

	/** What clients are sent of the trough and the net: all the renderer, goggles and screen read of them. */
	private void writeSummary(CompoundTag tag, HolderLookup.Provider registries) {
		summarise();
		if (!shownFood.isEmpty())
			tag.put("ShownFood", shownFood.save(registries));
		tag.putInt("FoodCount", shownFoodCount);
		ListTag shownTray = new ListTag();
		for (ItemStack stack : tray)
			shownTray.add(stack.save(registries));
		tag.put("Tray", shownTray);
		tag.putInt("TrayUsed", trayUsed);
	}

	private void readSummary(CompoundTag tag, HolderLookup.Provider registries) {
		shownFood = ItemStack.parseOptional(registries, tag.getCompound("ShownFood"));
		shownFoodCount = tag.getInt("FoodCount");
		tray.clear();
		for (Tag entry : tag.getList("Tray", Tag.TAG_COMPOUND))
			ItemStack.parse(registries, entry).ifPresent(tray::add);
		trayUsed = tag.getInt("TrayUsed");
	}

	private void onInventoryChanged() {
		setChanged();
		syncQueued = true;
		// a client's inventories change only through an open menu (or in Ponder): the trough and net follow them
		if (showsSummary())
			summarise();
	}

	@Override
	public void clearContent() {
		holdCaptive(ItemStack.EMPTY);
		adults = 0;
		blue = 0;
		young.clear();
		sample = null;
		createFailed = false;
		for (int i = 0; i < food.getSlots(); i++)
			food.setStackInSlot(i, ItemStack.EMPTY);
		for (int i = 0; i < output.getSlots(); i++)
			output.setStackInSlot(i, ItemStack.EMPTY);
		water.setFluid(FluidStack.EMPTY);
	}

	// ---------------------------------------------------------------- Fruitful on the item

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
		tag.putInt("Adults", adults);
		tag.putInt("Blue", blue);
		ListTag litters = new ListTag();
		for (int[] litter : young) {
			CompoundTag entry = new CompoundTag();
			entry.putInt("Count", litter[0]);
			entry.putInt("Ticks", litter[1]);
			entry.putInt("Blue", litter[2]);
			litters.add(entry);
		}
		tag.put("Young", litters);
		tag.putFloat("Progress", progress);
		if (clientPacket) {
			writeSummary(tag, registries);
			// the impact the network holds, settled, so the goggles agree with it
			if (!Float.isNaN(impact.applied()))
				tag.putFloat("Impact", impact.applied());
		} else if (!showsSummary()) {
			// a client's inventories aren't the tank's (a creative pick-block copy mustn't paste a stale trough and net)
			tag.put("Food", food.serializeNBT(registries));
			tag.put("Output", output.serializeNBT(registries));
		}
		tag.put("Water", water.writeToNBT(registries, new CompoundTag()));
		if (!enchantments.isEmpty())
			ItemEnchantments.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), enchantments)
				.ifSuccess(encoded -> tag.put("Enchantments", encoded));
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		ItemStack was = captive;
		// a sync that left the captive out (see getUpdatePacket): it's the one last sent, whatever a click here guessed;
		// and the same bottle again keeps the stack it has, so what's cached against it isn't made again
		if (clientPacket && tag.getBoolean("SameCaptive")) {
			if (receivedCaptive != null && captive != receivedCaptive)
				holdCaptive(receivedCaptive);
		} else {
			ItemStack read = ItemStack.parseOptional(registries, tag.getCompound("Captive"));
			if (!ItemStack.isSameItemSameComponents(captive, read))
				holdCaptive(read);
			if (clientPacket)
				receivedCaptive = captive;
		}
		if (clientPacket)
			syncedImpact = tag.contains("Impact") ? tag.getFloat("Impact") : Float.NaN;
		int wasBlue = blue, wasCount = getCount();
		adults = tag.getInt("Adults");
		blue = tag.getInt("Blue");
		young.clear();
		for (Tag entry : tag.getList("Young", Tag.TAG_COMPOUND))
			young.add(new int[] {((CompoundTag) entry).getInt("Count"), ((CompoundTag) entry).getInt("Ticks"), ((CompoundTag) entry).getInt("Blue")});
		if (!ItemStack.isSameItemSameComponents(was, captive) || clientPacket && (blue != wasBlue || getCount() < wasCount)) {
			sample = null;
			createFailed = false;
			shown.clear();
		}
		progress = tag.getFloat("Progress");
		if (clientPacket) {
			readSummary(tag, registries);
		} else {
			food.deserializeNBT(registries, tag.getCompound("Food"));
			output.deserializeNBT(registries, tag.getCompound("Output"));
			summarise();
		}
		water.readFromNBT(registries, tag.getCompound("Water"));
		enchantments = tag.contains("Enchantments")
			? ItemEnchantments.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("Enchantments"))
				.result().orElse(ItemEnchantments.EMPTY)
			: ItemEnchantments.EMPTY;
	}

	@Override
	public void sendData() {
		if (level != null) {
			long now = level.getGameTime();
			ownSyncAt = now;
			// the bottle goes with this sync unless an earlier one sent it, and again once that was a while ago
			if (captive != sentCaptive || now - sentCaptiveAt >= CAPTIVE_RESEND_TICKS) {
				sentCaptive = captive;
				sentCaptiveAt = now;
			}
		}
		syncQueued = false;
		super.sendData();
	}

	/**
	 * The changes this tank's own syncs send leave out the captive when it's the one clients were last sent (its bottle
	 * carries the whole creature). A client first seeing the tank gets everything, from {@code getUpdateTag}.
	 */
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this, (be, registries) -> ((FishTankBlockEntity) be).updatePacketTag(registries));
	}

	private CompoundTag updatePacketTag(HolderLookup.Provider registries) {
		long now = level == null ? 0 : level.getGameTime();
		// its own sync is broadcast at the start of the next tick; the bottle is left out only if an earlier one of its own
		// syncs sent it (building a packet doesn't count: only sendData, which broadcasts to every client watching)
		omitCaptive = now - ownSyncAt <= 1 && captive == sentCaptive && sentCaptiveAt < ownSyncAt;
		try {
			return writeClient(new CompoundTag(), registries);
		} finally {
			omitCaptive = false;
		}
	}

	// ---------------------------------------------------------------- handlers

	/** The screen's bottle slot: put a bottled creature in an empty tank, or take it back out. */
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
			if (!stack.isEmpty() && hasCreature() && ItemStack.isSameItemSameComponents(stack, captive))
				return;
			if (level == null || level.isClientSide) {
				holdCaptive(stack.copyWithCount(Math.min(1, stack.getCount())));
				adults = captive.isEmpty() ? 0 : Math.max(1, adults);
				sample = null;
				createFailed = false;
				shown.clear();
				return;
			}
			if (stack.isEmpty()) {
				release();
				return;
			}
			if (hasCreature())
				release();
			setCaptive(stack.copyWithCount(1));
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (hasCreature() || !isItemValid(slot, stack))
				return stack;
			if (!simulate)
				setStackInSlot(slot, stack);
			return stack.copyWithCount(stack.getCount() - 1);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			if (amount <= 0 || !hasCreature())
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
			return TankRules.livesInTank(stack, level);
		}
	}

	private class AutomationHandler implements IItemHandler {
		@Override
		public int getSlots() {
			return food.getSlots() + output.getSlots();
		}

		private boolean isFood(int slot) {
			return slot < food.getSlots();
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return isFood(slot) ? food.getStackInSlot(slot) : output.getStackInSlot(slot - food.getSlots());
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			return isFood(slot) ? food.insertItem(slot, stack, simulate) : stack;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return isFood(slot) ? ItemStack.EMPTY : output.extractItem(slot - food.getSlots(), amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			return 64;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return isFood(slot) && stack.is(ModItems.FISH_FOOD.get());
		}
	}
}
