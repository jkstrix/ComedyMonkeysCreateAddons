package com.comedymonkey.createaddons.content.pen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.FixedStress;
import com.comedymonkey.createaddons.content.MachineEnchantments;
import com.comedymonkey.createaddons.content.chest.ChestStacks;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Keeps one captured animal (the Bottled Animal it came in) and a herd of it, adults and young, as a vanilla pen would.
 * While the shaft turns and the trough has its food, every five minutes each adult eats one food and has a baby (one
 * adult is enough to start); each litter grows up twenty minutes after it's born, up to the pen's capacity. Only
 * adults breed, give drops and XP, grow wool, lay eggs (armadillos shed scutes) or make milk, all only while it turns. A sword takes half the animals (rounded to the nearest, never the last), giving their drops as if killed by
 * whoever holds it (so Looting counts); shears take the wool of every sheep at once; a bucket or bowl takes milk or
 * stew. Fruitful on the pen's item makes it breed faster. It puts the same stress on the network at any speed, and
 * none while empty.
 */
public class AnimalPenBlockEntity extends KineticBlockEntity implements Clearable, MenuProvider {
	/** Animals the window shows at most. */
	public static final int SHOWN = 4;
	/** Animals each pen holds. */
	public static final int CAPACITY = 128, BRASS_CAPACITY = 512;
	/** The trough holds food for every animal, in normal stacks: 2 slots on the Animal Pen, 8 on the Brass one. */
	public static final int TROUGH_SLOTS = CAPACITY / 64, BRASS_TROUGH_SLOTS = BRASS_CAPACITY / 64;
	private static final int SYNC_INTERVAL = 10;

	private ItemStack captive = ItemStack.EMPTY;
	/** The item's enchantments (Fruitful), kept so the block breaks back into the same item. */
	private ItemEnchantments enchantments = ItemEnchantments.EMPTY;
	private int adults;
	/** Babies by when they were born, oldest first; each litter grows up when its time runs out. */
	private final List<Litter> young = new ArrayList<>();
	private final FoodHandler food;
	/** Food from an older save that no longer fits the trough; put out with the output on the next tick. */
	private final List<ItemStack> spilled = new ArrayList<>();
	private final ItemStackHandler output;
	private final FluidTank milk;
	private final IItemHandler automation = new AutomationHandler();
	private final IItemHandlerModifiable captiveSlot = new CaptiveHandler();
	private final IFluidHandler milkOut = new MilkOut();
	private int breedTicks;
	private boolean woolReady = true;
	private int woolTicks;
	private float eggProgress;
	private float milkProgress;
	private boolean syncQueued;
	/** One of the animals, not in the world: what it eats, drops and makes. Made from the bottle when needed. */
	@Nullable
	private Entity sample;
	/** What {@link #getFoodHint} found for the current animal; null until looked for. */
	@Nullable
	private ItemStack foodHint;
	/**
	 * Server: the captive as last sent to clients. Its bottle carries the whole animal's data, so the frequent syncs
	 * (milk, eggs, food) leave it out until it changes; it's only ever replaced, never changed in place.
	 */
	@Nullable
	private ItemStack sentCaptive;
	/**
	 * When it was last sent; it goes again now and then anyway, to put right a screen's guess (see CaptiveHandler) or a
	 * client copy that started over.
	 */
	private long sentCaptiveAt;
	private static final int CAPTIVE_RESEND_TICKS = 100;
	/**
	 * Game time of this pen's own last sync. Only the packet that sends goes without the captive: anything else asking
	 * (a refused break putting the block back on one player's screen) may be filling in a fresh client copy.
	 */
	private long ownSyncAt = Long.MIN_VALUE / 2;
	/** Set while writing a sync that leaves the captive out. */
	private boolean omitCaptive;
	/** Client only: the animals walking about in the window. */
	private final List<PenAnimal> shown = new ArrayList<>();

	public AnimalPenBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ANIMAL_PEN.get(), pos, state);
		boolean brass = isBrass(state);
		food = new FoodHandler(brass ? BRASS_TROUGH_SLOTS : TROUGH_SLOTS);
		output = new ItemStackHandler(brass ? 18 : 9) {
			@Override
			protected void onContentsChanged(int slot) {
				onInventoryChanged();
			}
		};
		milk = new FluidTank(brass ? 32_000 : 8_000, stack -> stack.getFluid().isSame(NeoForgeMod.MILK.get())) {
			@Override
			protected void onContentsChanged() {
				onInventoryChanged();
			}
		};
	}

	private static boolean isBrass(BlockState state) {
		return state.getBlock() instanceof AnimalPenBlock pen && pen.isBrass();
	}

	public boolean isBrass() {
		return isBrass(getBlockState());
	}

	public int getCapacity() {
		return isBrass() ? BRASS_CAPACITY : CAPACITY;
	}

	/** Ticks a birth takes, sped up by Fruitful. */
	private int breedTime() {
		int base = 20 * CMConfig.Server.get(CMConfig.SERVER.penBreedSeconds);
		return Math.max(1, Math.round(base / MachineEnchantments.speedMultiplier(getFruitfulLevel(),
			CMConfig.SERVER.penFruitfulBonus)));
	}

	/** Ticks a baby takes to grow up, sped up by Fruitful. */
	private int growTime() {
		int base = 20 * CMConfig.Server.get(CMConfig.SERVER.penGrowSeconds);
		return Math.max(1, Math.round(base / MachineEnchantments.speedMultiplier(getFruitfulLevel(),
			CMConfig.SERVER.penFruitfulBonus)));
	}

	public int getFruitfulLevel() {
		return MachineEnchantments.levelIn(enchantments, MachineEnchantments.FRUITFUL);
	}

	// ---------------------------------------------------------------- the animal

	public boolean hasAnimal() {
		return !captive.isEmpty() && getCount() > 0;
	}

	/** Adults and young together. */
	public int getCount() {
		return adults + getYoung();
	}

	public int getAdults() {
		return adults;
	}

	public int getYoung() {
		int babies = 0;
		for (Litter litter : young)
			babies += litter.count;
		return babies;
	}

	public ItemStack getCaptive() {
		return captive;
	}

	/** Ponder only: this animal, with so many adults and young. */
	public void setPonderHerd(ItemStack bottle, int grown, int babies) {
		if (BottledAnimalItem.typeOf(bottle) != BottledAnimalItem.typeOf(captive)) {
			sample = null;
			foodHint = null;
			shown.clear();
		}
		captive = bottle.copyWithCount(1);
		adults = grown;
		young.clear();
		if (babies > 0)
			young.add(new Litter(babies, growTime()));
	}

	/** Ponder only: what's in the trough. */
	public void setPonderFood(ItemStack stack) {
		for (int i = 0; i < food.getSlots(); i++)
			food.setStackInSlot(i, ItemStack.EMPTY);
		food.spread(List.of(stack.copy()));
	}

	@Nullable
	public Entity sample() {
		if (sample == null && hasAnimal() && level != null) {
			sample = BottledAnimalItem.create(captive, level).orElse(null);
			if (sample instanceof AgeableMob grown)
				grown.setAge(0);
			if (sample != null)
				sample.setPos(Vec3.atCenterOf(worldPosition));
			if (sample instanceof Sheep sheep)
				sheep.setSheared(!woolReady);
		}
		return sample;
	}

	/** Whether this is the captured animal's food (what it's bred with). */
	public boolean isFood(ItemStack stack) {
		return !stack.isEmpty() && sample() instanceof Animal animal && animal.isFood(stack);
	}

	/** Some of the animal's own food (food left from another animal doesn't count). */
	private boolean hasFood() {
		return getFoodCount() > 0;
	}

	/** How much of the animal's own food the trough holds, over all its slots. */
	public int getFoodCount() {
		int count = 0;
		for (int i = 0; i < food.getSlots(); i++)
			if (isFood(food.getStackInSlot(i)))
				count += food.getStackInSlot(i).getCount();
		return count;
	}

	/** The food to show lying in the trough: the first stack in it, or empty. */
	public ItemStack getTroughFood() {
		for (int i = 0; i < food.getSlots(); i++)
			if (!food.getStackInSlot(i).isEmpty())
				return food.getStackInSlot(i);
		return ItemStack.EMPTY;
	}

	/** Eats {@code amount} of the animal's food, from whichever slots have it. */
	private void eat(int amount) {
		for (int i = 0; i < food.getSlots() && amount > 0; i++)
			if (isFood(food.getStackInSlot(i)))
				amount -= food.extractItem(i, amount, false).getCount();
	}

	/** One of the animal's breeding foods, to show in the empty food slot; found once per animal. */
	public ItemStack getFoodHint() {
		if (foodHint == null)
			foodHint = findFood();
		return foodHint;
	}

	private ItemStack findFood() {
		if (!(sample() instanceof Animal))
			return ItemStack.EMPTY;
		// The usual foods first, then anything at all (modded animals)
		for (Item item : new Item[] {Items.WHEAT, Items.WHEAT_SEEDS, Items.CARROT, Items.POTATO, Items.BEETROOT,
			Items.GOLDEN_CARROT, Items.HAY_BLOCK, Items.SWEET_BERRIES, Items.SEAGRASS, Items.COD, Items.SLIME_BALL,
			Items.BAMBOO, Items.CACTUS, Items.DANDELION, Items.TORCHFLOWER_SEEDS, Items.SPIDER_EYE, Items.WARPED_FUNGUS,
			Items.CRIMSON_FUNGUS, Items.KELP, Items.TROPICAL_FISH_BUCKET, Items.BEEF}) {
			ItemStack stack = new ItemStack(item);
			if (isFood(stack))
				return stack;
		}
		for (Item item : BuiltInRegistries.ITEM) {
			ItemStack stack = new ItemStack(item);
			if (isFood(stack))
				return stack;
		}
		return ItemStack.EMPTY;
	}

	public boolean isMilkable() {
		return sample() instanceof Cow || sample() instanceof Goat;
	}

	/** What the animal lays or sheds by itself, or nothing. */
	@Nullable
	private Item layItem() {
		Entity animal = sample();
		if (animal instanceof Chicken)
			return Items.EGG;
		if (animal instanceof Armadillo)
			return Items.ARMADILLO_SCUTE;
		return null;
	}

	private void setCaptive(ItemStack bottle) {
		captive = bottle;
		adults = 0;
		young.clear();
		if (!bottle.isEmpty()) {
			// A bottled baby arrives a baby, with as long to grow up as it had left
			CustomData data = bottle.get(DataComponents.ENTITY_DATA);
			int age = data == null ? 0 : data.copyTag().getInt("Age");
			if (age < 0)
				young.add(new Litter(1, Math.min(-age, growTime())));
			else
				adults = 1;
		}
		sample = null;
		foodHint = null;
		shown.clear();
		// Food left from the last animal that this one doesn't eat goes out with the rest of the output
		if (!bottle.isEmpty() && level != null && !level.isClientSide)
			for (int i = 0; i < food.getSlots(); i++) {
				ItemStack left = food.getStackInSlot(i);
				if (!left.isEmpty() && !isFood(left)) {
					food.setStackInSlot(i, ItemStack.EMPTY);
					store(List.of(left));
				}
			}
		breedTicks = 0;
		woolReady = true;
		woolTicks = 0;
		eggProgress = 0;
		milkProgress = 0;
		setChanged();
		sendData();
		if (level != null)
			level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
	}

	/** Takes the captured animal back out in its bottle; the rest are butchered into the output. */
	private ItemStack release() {
		if (!hasAnimal())
			return ItemStack.EMPTY;
		// Only adults leave anything; the one in the bottle goes back out with it
		if (level instanceof ServerLevel server)
			store(butcher(server, Math.max(0, adults - 1), null, ItemStack.EMPTY));
		ItemStack out = captive;
		setCaptive(ItemStack.EMPTY);
		return out;
	}

	// ---------------------------------------------------------------- using it

	/** A click with an item (by a player or a Deployer); false if the item does nothing here. */
	boolean use(Player player, InteractionHand hand, ItemStack stack) {
		boolean client = level == null || level.isClientSide;
		if (!hasAnimal()) {
			if (!(stack.getItem() instanceof BottledAnimalItem) || BottledAnimalItem.typeOf(stack) == null)
				return false;
			if (!client)
				setCaptive(stack.split(1));
			return true;
		}
		if (stack.is(ItemTags.SWORDS)) {
			if (!client)
				swipe(player, hand, stack);
			return true;
		}
		if (stack.is(Tags.Items.TOOLS_SHEAR) && sample() instanceof Sheep) {
			if (!client)
				shear(player, hand, stack);
			return true;
		}
		if (stack.is(Items.BUCKET) && isMilkable() && milk.getFluidAmount() >= 1000) {
			if (!client)
				takeMilk(player, hand, stack, 1000, new ItemStack(Items.MILK_BUCKET), SoundEvents.COW_MILK);
			return true;
		}
		if (stack.is(Items.BOWL) && sample() instanceof MushroomCow && milk.getFluidAmount() >= 250) {
			if (!client)
				takeMilk(player, hand, stack, 250, new ItemStack(Items.MUSHROOM_STEW), SoundEvents.MOOSHROOM_MILK);
			return true;
		}
		if (isFood(stack)) {
			if (!client) {
				ItemStack left = ItemHandlerHelper.insertItemStacked(food, stack.copy(), false);
				if (!player.getAbilities().instabuild)
					stack.setCount(left.getCount());
			}
			return true;
		}
		return false;
	}

	/**
	 * Half the adults (rounded to the nearest, always leaving one to breed) are killed by whoever holds the sword. The
	 * young are left alone to grow up.
	 */
	private void swipe(Player player, InteractionHand hand, ItemStack sword) {
		if (!(level instanceof ServerLevel server))
			return;
		int killedAdults = Math.min(Math.round(adults / 2f), adults - 1);
		if (killedAdults <= 0) {
			player.displayClientMessage(lang().translate(adults == 0 ? "gui.animal_pen.too_young_cull"
				: "gui.animal_pen.last_one").component(), true);
			return;
		}
		adults -= killedAdults;
		store(butcher(server, killedAdults, player, sword));
		ModAdvancements.award(player, ModAdvancements.PEN_CULL);
		if (!(player instanceof FakePlayer)) {
			int xp = 0;
			for (int i = 0; i < killedAdults; i++)
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

	/** The drops of {@code killed} animals, as if killed by {@code killer} with {@code weapon} (Looting, Fire Aspect). */
	private List<ItemStack> butcher(ServerLevel server, int killed, @Nullable Player killer, ItemStack weapon) {
		List<ItemStack> drops = new ArrayList<>();
		if (killed <= 0 || !(sample() instanceof LivingEntity animal))
			return drops;
		LootTable table = server.getServer().reloadableRegistries().getLootTable(animal.getLootTable());
		DamageSource source = killer != null ? server.damageSources().playerAttack(killer) : server.damageSources().generic();
		boolean burning = !weapon.isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(
			server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FIRE_ASPECT), weapon) > 0;
		if (burning)
			animal.igniteForSeconds(4);
		for (int i = 0; i < killed; i++) {
			LootParams.Builder params = new LootParams.Builder(server)
				.withParameter(LootContextParams.THIS_ENTITY, animal)
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
			animal.clearFire();
		return drops;
	}

	private void shear(Player player, InteractionHand hand, ItemStack shears) {
		if (!(level instanceof ServerLevel server) || !(sample() instanceof Sheep sheep))
			return;
		if (!woolReady) {
			player.displayClientMessage(lang().translate("gui.animal_pen.wool_growing").component(), true);
			return;
		}
		Item wool = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(sheep.getColor().getName() + "_wool"));
		List<ItemStack> drops = new ArrayList<>();
		int total = 0;
		if (adults == 0) {
			player.displayClientMessage(lang().translate("gui.animal_pen.too_young").component(), true);
			return;
		}
		for (int i = 0; i < adults; i++)
			total += 1 + server.random.nextInt(3);
		while (total > 0) {
			int n = Math.min(total, 64);
			drops.add(new ItemStack(wool, n));
			total -= n;
		}
		store(drops);
		woolReady = false;
		woolTicks = 0;
		sheep.setSheared(true);
		shears.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
		server.playSound(null, worldPosition, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1, 1);
		setChanged();
		sendData();
	}

	private void takeMilk(Player player, InteractionHand hand, ItemStack container, int amount, ItemStack filled,
						  net.minecraft.sounds.SoundEvent sound) {
		milk.drain(amount, IFluidHandler.FluidAction.EXECUTE);
		player.setItemInHand(hand, ItemUtils.createFilledResult(container, player, filled));
		level.playSound(null, worldPosition, sound, SoundSource.PLAYERS, 1, 1);
	}

	/** Into the output; what doesn't fit drops out of the front. */
	private void store(List<ItemStack> drops) {
		for (ItemStack drop : drops) {
			ItemStack left = ItemHandlerHelper.insertItemStacked(output, drop, false);
			if (!left.isEmpty() && level != null) {
				Vec3 at = frontOf(0.6);
				Containers.dropItemStack(level, at.x, at.y, at.z, left);
			}
		}
	}

	/** A point just in front of the window, {@code out} blocks from the block's centre. */
	private Vec3 frontOf(double out) {
		Direction front = getBlockState().getValue(AnimalPenBlock.FACING);
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
		if (!spilled.isEmpty()) {
			store(List.copyOf(spilled));
			spilled.clear();
		}
		if (FixedStress.keep(this, wantedImpact()))
			sendData();
		if (isRunning())
			work();
		if (syncQueued && level.getGameTime() % SYNC_INTERVAL == 0) {
			syncQueued = false;
			sendData();
		}
	}

	private void work() {
		// Growing up: each litter becomes adults when its time runs out
		boolean grew = false;
		for (int i = 0; i < young.size(); i++) {
			Litter litter = young.get(i);
			if (--litter.ticksLeft <= 0) {
				adults += litter.count;
				young.remove(i--);
				grew = true;
			}
		}
		if (grew) {
			setChanged();
			sendData();
		}
		// Breeding: every adult eats one food and has a baby (as many as the food and room allow)
		if (canBreed()) {
			if (++breedTicks >= breedTime()) {
				breedTicks = 0;
				int born = Math.min(Math.min(adults, getCapacity() - getCount()), getFoodCount());
				eat(born);
				young.add(new Litter(born, growTime()));
				if (getCount() >= getCapacity())
					ModAdvancements.awardNearby(level, worldPosition,
						isBrass() ? ModAdvancements.BRASS_PEN_FULL : ModAdvancements.PEN_FULL, captive, getCount());
				level.playSound(null, worldPosition, SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 0.4f, 1.6f);
				setChanged();
				sendData();
				level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
			}
		}
		if (sample() instanceof Sheep sheep && !woolReady
			&& ++woolTicks >= 20 * CMConfig.Server.get(CMConfig.SERVER.penWoolSeconds)) {
			woolReady = true;
			sheep.setSheared(false);
			sendData();
		}
		Item lays = layItem();
		if (lays != null) {
			int every = 20 * CMConfig.Server.get(CMConfig.SERVER.penEggSeconds);
			eggProgress += adults;
			while (eggProgress >= every) {
				ItemStack left = ItemHandlerHelper.insertItemStacked(output, new ItemStack(lays), false);
				if (!left.isEmpty()) {
					// No room: wait with the next one ready
					eggProgress = every;
					break;
				}
				eggProgress -= every;
			}
		}
		if (isMilkable()) {
			milkProgress += adults * CMConfig.Server.get(CMConfig.SERVER.penMilkPerMinute) / 1200f;
			int whole = (int) milkProgress;
			if (whole > 0) {
				milk.fill(new FluidStack(NeoForgeMod.MILK.get(), whole), IFluidHandler.FluidAction.EXECUTE);
				milkProgress -= whole;
			}
		}
	}

	/** Turning, not overstressed, with an animal in it. */
	public boolean isRunning() {
		return getSpeed() != 0 && !isOverStressed() && hasAnimal();
	}

	@Override
	public float calculateStressApplied() {
		float impact = wantedImpact();
		lastStressApplied = impact;
		return impact;
	}

	private float wantedImpact() {
		int su = CMConfig.Server.get(isBrass() ? CMConfig.SERVER.brassPenStress : CMConfig.SERVER.penStress);
		return FixedStress.impact(this, su, !hasAnimal());
	}

	@Override
	public void onSpeedChanged(float previousSpeed) {
		super.onSpeedChanged(previousSpeed);
		if (level != null && !level.isClientSide && FixedStress.keep(this, wantedImpact()))
			sendData();
	}

	// ---------------------------------------------------------------- the window (client)

	/** The animals shown walking about, at most {@link #SHOWN}: grown and young in the same mix as the herd. */
	public List<PenAnimal> getShown() {
		return shown;
	}

	private void clientTick() {
		// Count towards the next birth in step with the server (which only sends it now and then), so the gauge
		// and percentage climb smoothly; the server's count replaces this whenever it arrives
		if (isBreeding())
			breedTicks = Math.min(breedTicks + 1, breedTime() - 1);
		// The same for the young growing up (the "grows up in" time); growing up itself is left to the server
		if (isRunning())
			for (Litter litter : young)
				litter.ticksLeft = Math.max(1, litter.ticksLeft - 1);
		int total = getCount();
		int want = hasAnimal() ? Math.min(total, SHOWN) : 0;
		while (shown.size() > want)
			shown.removeLast();
		while (shown.size() < want) {
			Entity made = BottledAnimalItem.create(captive, level).orElse(null);
			if (made == null)
				break;
			shown.add(new PenAnimal(made, level.random));
		}
		int babies = getYoung() == 0 ? 0 : adults == 0 ? shown.size()
			: Mth.clamp(Math.round(shown.size() * getYoung() / (float) total), 1, Math.max(1, shown.size() - 1));
		for (int i = 0; i < shown.size(); i++) {
			PenAnimal animal = shown.get(i);
			animal.setBaby(i >= shown.size() - babies);
			if (animal.entity() instanceof Sheep sheep)
				sheep.setSheared(!woolReady);
			animal.tick(level.random, isRunning());
		}
	}

	// ---------------------------------------------------------------- the wrench screen and readouts

	@Override
	public Component getDisplayName() {
		return getBlockState().getBlock().getName();
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new AnimalPenMenu(containerId, inventory, this);
	}

	/**
	 * The screen finds this block by its position, sent with every opening however it's opened (a spectator's
	 * right-click or another mod calling openMenu sends nothing else).
	 */
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

	public FluidTank getMilk() {
		return milk;
	}

	/** How far to the next birth, 0 to 1. */
	public float getBreedProgress() {
		return Mth.clamp(breedTicks / (float) Math.max(1, breedTime()), 0, 1);
	}

	public boolean isWoolReady() {
		return woolReady;
	}

	/** Ticks until the next litter grows up, or 0 with no young. */
	public int ticksToNextGrown() {
		int next = 0;
		for (Litter litter : young)
			next = next == 0 ? litter.ticksLeft : Math.min(next, litter.ticksLeft);
		return Math.max(0, next);
	}

	public Component getStatus() {
		if (!hasAnimal())
			return lang().translate("gui.animal_pen.empty").component();
		if (getSpeed() == 0 || isOverStressed())
			return lang().translate("gui.animal_pen.idle").component();
		if (getCount() >= getCapacity())
			return lang().translate("gui.animal_pen.full").component();
		if (adults == 0)
			return lang().translate("gui.animal_pen.growing").component();
		if (!hasFood())
			return lang().translate("gui.animal_pen.hungry").component();
		return lang().translate("gui.animal_pen.breeding", Math.round(100 * getBreedProgress())).component();
	}

	/** Adults, room and food to breed. */
	private boolean canBreed() {
		return adults > 0 && getCount() < getCapacity() && hasFood();
	}

	/** Whether the status line is good news (it's breeding). */
	public boolean isBreeding() {
		return isRunning() && canBreed();
	}

	/** "Cow: 40 + 24 young / 128" */
	public Component getHerd() {
		Component name = BottledAnimalItem.typeOf(captive) instanceof net.minecraft.world.entity.EntityType<?> type
			? type.getDescription() : Component.empty();
		int babies = getYoung();
		return babies > 0
			? lang().translate("gui.animal_pen.herd_young", name, adults, babies, getCapacity()).component()
			: lang().translate("gui.animal_pen.herd", name, adults, getCapacity()).component();
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		lang().translate("gui.goggles.animal_pen").forGoggles(tooltip);
		if (!hasAnimal()) {
			lang().translate("gui.animal_pen.empty").style(ChatFormatting.RED).forGoggles(tooltip, 1);
		} else {
			lang().add(getHerd().copy()).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
			lang().add(getStatus().copy()).style(isBreeding() ? ChatFormatting.AQUA : ChatFormatting.RED).forGoggles(tooltip, 1);
			int fruitful = getFruitfulLevel();
			if (fruitful > 0)
				lang().translate("gui.animal_pen.fruitful", Component.translatable("enchantment.level." + fruitful))
					.style(ChatFormatting.LIGHT_PURPLE).forGoggles(tooltip, 1);
			if (sample() instanceof Sheep)
				lang().translate(woolReady ? "gui.animal_pen.wool_ready" : "gui.animal_pen.wool_growing")
					.style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
			if (isMilkable())
				lang().text(String.format(Locale.ROOT, "%,d / %,d mB ", milk.getFluidAmount(), milk.getCapacity()))
					.style(ChatFormatting.GRAY).add(lang().add(milk.getFluid().isEmpty()
						? new FluidStack(NeoForgeMod.MILK.get(), 1).getHoverName() : milk.getFluid().getHoverName()))
					.forGoggles(tooltip, 1);
		}
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		return true;
	}

	private static LangBuilder lang() {
		return new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID);
	}

	// ---------------------------------------------------------------- output and dropping

	public IItemHandler getAutomationHandler() {
		return automation;
	}

	public IFluidHandler getMilkHandler() {
		return milkOut;
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

	/** Broken: the bottle, the food and the output, and the extra animals as their drops. */
	void dropContents() {
		if (!(level instanceof ServerLevel server))
			return;
		List<ItemStack> all = new ArrayList<>();
		if (hasAnimal())
			all.addAll(butcher(server, Math.max(0, adults - 1), null, ItemStack.EMPTY));
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
		// Clamped: a herd from when the pens held more (they used to be configurable) can be over capacity
		return hasAnimal() ? Mth.clamp(Math.round(15f * getCount() / getCapacity()), 1, 15) : 0;
	}

	private void onInventoryChanged() {
		setChanged();
		syncQueued = true;
	}

	/** Called by block movers once they've copied this block entity elsewhere, so nothing is dropped twice. */
	@Override
	public void clearContent() {
		captive = ItemStack.EMPTY;
		adults = 0;
		young.clear();
		sample = null;
		for (int i = 0; i < food.getSlots(); i++)
			food.setStackInSlot(i, ItemStack.EMPTY);
		spilled.clear();
		for (int i = 0; i < output.getSlots(); i++)
			output.setStackInSlot(i, ItemStack.EMPTY);
		milk.setFluid(FluidStack.EMPTY);
	}

	// ---------------------------------------------------------------- the item's enchantments

	@Override
	protected void applyImplicitComponents(DataComponentInput input) {
		super.applyImplicitComponents(input);
		enchantments = input.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
	}

	/** So the block breaks back into the enchanted item (see its loot table's copy_components). */
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
		ListTag litters = new ListTag();
		for (Litter litter : young) {
			CompoundTag entry = new CompoundTag();
			entry.putInt("Count", litter.count);
			entry.putInt("Ticks", litter.ticksLeft);
			litters.add(entry);
		}
		tag.put("Young", litters);
		tag.put("Food", food.serializeNBT(registries));
		tag.put("Output", output.serializeNBT(registries));
		tag.put("Milk", milk.writeToNBT(registries, new CompoundTag()));
		tag.putInt("BreedTicks", breedTicks);
		tag.putBoolean("WoolReady", woolReady);
		tag.putInt("WoolTicks", woolTicks);
		tag.putFloat("EggProgress", eggProgress);
		tag.putFloat("MilkProgress", milkProgress);
		if (!enchantments.isEmpty())
			ItemEnchantments.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), enchantments)
				.ifSuccess(encoded -> tag.put("Enchantments", encoded));
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		// A sync that left the captive out (see getUpdatePacket): it's the one already here
		if (!(clientPacket && tag.getBoolean("SameCaptive"))) {
			ItemStack was = captive;
			captive = ItemStack.parseOptional(registries, tag.getCompound("Captive"));
			if (!ItemStack.isSameItemSameComponents(was, captive)) {
				sample = null;
				foodHint = null;
				shown.clear();
			}
		}
		// Saved before pens had young: everything counts as grown
		adults = tag.contains("Adults") ? tag.getInt("Adults") : tag.getInt("Count");
		young.clear();
		for (Tag entry : tag.getList("Young", Tag.TAG_COMPOUND))
			young.add(new Litter(((CompoundTag) entry).getInt("Count"), ((CompoundTag) entry).getInt("Ticks")));
		food.load(tag.get("Food"), registries);
		output.deserializeNBT(registries, tag.getCompound("Output"));
		milk.readFromNBT(registries, tag.getCompound("Milk"));
		breedTicks = tag.getInt("BreedTicks");
		woolReady = !tag.contains("WoolReady") || tag.getBoolean("WoolReady");
		woolTicks = tag.getInt("WoolTicks");
		eggProgress = tag.getFloat("EggProgress");
		milkProgress = tag.getFloat("MilkProgress");
		enchantments = tag.contains("Enchantments")
			? ItemEnchantments.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("Enchantments"))
				.result().orElse(ItemEnchantments.EMPTY)
			: ItemEnchantments.EMPTY;
		if (sample instanceof Sheep sheep)
			sheep.setSheared(!woolReady);
	}

	@Override
	public void sendData() {
		if (level != null)
			ownSyncAt = level.getGameTime();
		super.sendData();
	}

	/**
	 * The changes this pen's own syncs send leave out the captive when it's the one clients were last sent. A client
	 * first seeing the pen gets everything, from {@code getUpdateTag}, and so does anything else asking.
	 */
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this, (be, registries) -> ((AnimalPenBlockEntity) be).updatePacketTag(registries));
	}

	private CompoundTag updatePacketTag(HolderLookup.Provider registries) {
		long now = level == null ? 0 : level.getGameTime();
		// its own sync is broadcast at the start of the next tick
		omitCaptive = now - ownSyncAt <= 1 && captive == sentCaptive && now - sentCaptiveAt < CAPTIVE_RESEND_TICKS;
		try {
			return writeClient(new CompoundTag(), registries);
		} finally {
			if (!omitCaptive) {
				sentCaptive = captive;
				sentCaptiveAt = now;
			}
			omitCaptive = false;
		}
	}

	// ---------------------------------------------------------------- handlers

	/** Babies born together, and the ticks until they grow up. */
	private static final class Litter {
		int count;
		int ticksLeft;

		Litter(int count, int ticksLeft) {
			this.count = count;
			this.ticksLeft = ticksLeft;
		}
	}

	/** The trough: normal stacks of the animal's food, enough slots to feed every animal the pen can hold. */
	private class FoodHandler extends ItemStackHandler {
		FoodHandler(int slots) {
			super(slots);
		}

		/** A normal stack (vanilla's default would allow 99), so the slots hold exactly a food per animal. */
		@Override
		public int getSlotLimit(int slot) {
			return 64;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return isFood(stack);
		}

		@Override
		protected void onContentsChanged(int slot) {
			onInventoryChanged();
		}

		/**
		 * Loads the trough, from this format or an older one (one slot, which could hold a big stack, saved as a
		 * list like the upgraded chests' stacks or as a plain handler). Anything that no longer fits is spilled.
		 */
		void load(@Nullable Tag tag, HolderLookup.Provider registries) {
			List<ItemStack> loaded = new ArrayList<>();
			if (tag instanceof CompoundTag compound) {
				ItemStackHandler saved = new ItemStackHandler();
				saved.deserializeNBT(registries, compound);
				for (int i = 0; i < saved.getSlots(); i++)
					loaded.add(saved.getStackInSlot(i));
			} else if (tag instanceof ListTag list) {
				NonNullList<ItemStack> saved = NonNullList.withSize(Math.max(1, list.size()), ItemStack.EMPTY);
				ChestStacks.load(saved, list, registries);
				loaded.addAll(saved);
			}
			for (int i = 0; i < stacks.size(); i++)
				stacks.set(i, ItemStack.EMPTY);
			spilled.addAll(spread(loaded));
		}

		/** Puts the stacks into the slots in normal-sized stacks, returning what didn't fit. */
		List<ItemStack> spread(List<ItemStack> from) {
			List<ItemStack> left = new ArrayList<>();
			int next = 0;
			for (ItemStack stack : from) {
				stack = stack.copy();
				while (!stack.isEmpty() && next < stacks.size())
					stacks.set(next++, stack.split(Math.min(stack.getMaxStackSize(), getSlotLimit(0))));
				if (!stack.isEmpty())
					left.add(stack);
			}
			return left;
		}
	}

	/**
	 * The screen's animal slot: put a bottle in an empty pen, or take the animal back out. It hands out a copy of the
	 * bottle, because shift-clicking moves the slot's own stack away before telling it the slot is empty; with the
	 * real one gone the pen would never get to butcher the rest and empty itself.
	 */
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
			// The same bottle put back: clicking the slot with an identical bottle sets it again even though nothing
			// fits (vanilla's Slot.safeInsert), which mustn't butcher the herd and catch the animal again
			if (!stack.isEmpty() && hasAnimal() && ItemStack.isSameItemSameComponents(stack, captive))
				return;
			if (level == null || level.isClientSide) {
				// The client's guess until the server's state arrives
				captive = stack.copyWithCount(Math.min(1, stack.getCount()));
				if (captive.isEmpty()) {
					adults = 0;
					young.clear();
				} else if (getCount() == 0) {
					adults = 1;
				}
				sample = null;
				foodHint = null;
				shown.clear();
				return;
			}
			if (stack.isEmpty()) {
				release();
				return;
			}
			// Swapping one bottle for another in the screen: the old bottle is already on the cursor (a copy of it,
			// see getStackInSlot), so the old herd is released here and its bottle dropped, then the new one moves in
			if (hasAnimal())
				release();
			setCaptive(stack.copyWithCount(1));
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (hasAnimal() || !isItemValid(slot, stack))
				return stack;
			if (!simulate)
				setStackInSlot(slot, stack);
			return stack.copyWithCount(stack.getCount() - 1);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			if (amount <= 0 || !hasAnimal())
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
			return stack.getItem() instanceof BottledAnimalItem && BottledAnimalItem.typeOf(stack) != null;
		}
	}

	/** Automation: food in through the trough's slots from any side, what it's made out of the rest. */
	private class AutomationHandler implements IItemHandler {
		@Override
		public int getSlots() {
			return food.getSlots() + output.getSlots();
		}

		private boolean isTrough(int slot) {
			return slot < food.getSlots();
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return isTrough(slot) ? food.getStackInSlot(slot) : output.getStackInSlot(slot - food.getSlots());
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			return isTrough(slot) ? food.insertItem(slot, stack, simulate) : stack;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return isTrough(slot) ? ItemStack.EMPTY : output.extractItem(slot - food.getSlots(), amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			return 64;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return isTrough(slot) && isFood(stack);
		}
	}

	/** Pipes take milk out; nothing goes in. */
	private class MilkOut implements IFluidHandler {
		@Override
		public int getTanks() {
			return 1;
		}

		@Override
		public FluidStack getFluidInTank(int tank) {
			return milk.getFluid();
		}

		@Override
		public int getTankCapacity(int tank) {
			return milk.getCapacity();
		}

		@Override
		public boolean isFluidValid(int tank, FluidStack stack) {
			return false;
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return 0;
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			return milk.drain(resource, action);
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return milk.drain(maxDrain, action);
		}
	}
}
