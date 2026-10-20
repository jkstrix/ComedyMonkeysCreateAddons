package com.comedymonkey.createaddons.content.pen;

import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * An animal caught in a Capture Bottle (see {@link AnimalCapture}), drawn inside the jar by its own model. It keeps everything about the animal (its colour,
 * name, variant) in the stack's entity data, goes into an Animal Pen, or lets the animal back out when used on a block.
 */
public class BottledAnimalItem extends Item {
	public BottledAnimalItem(Properties properties) {
		super(properties);
	}

	/** A bottle holding this animal; it's removed from the world by the caller. */
	public static ItemStack of(Entity animal) {
		CompoundTag tag = new CompoundTag();
		animal.saveWithoutId(tag);
		tag.putString("id", EntityType.getKey(animal.getType()).toString());
		for (String key : new String[] {"Pos", "Motion", "Rotation", "UUID", "leash", "Leash", "FallDistance", "Fire", "OnGround"})
			tag.remove(key);
		ItemStack stack = new ItemStack(ModItems.BOTTLED_ANIMAL.get());
		stack.set(DataComponents.ENTITY_DATA, CustomData.of(tag));
		return stack;
	}

	@Nullable
	public static EntityType<?> typeOf(ItemStack stack) {
		CustomData data = stack.get(DataComponents.ENTITY_DATA);
		if (data == null)
			return null;
		// byString, not by(tag): a malformed id is just no animal rather than an exception
		return EntityType.byString(data.copyTag().getString("id")).orElse(null);
	}

	/**
	 * The animal in the bottle as a new entity (not yet in any world), or empty if it can't be made. Only something
	 * the bottle could have caught comes out: a stack edited to hold any other entity (a command block minecart, say)
	 * makes nothing, as vanilla's spawn eggs refuse such data from non-operators.
	 */
	public static Optional<Entity> create(ItemStack stack, Level level) {
		CustomData data = stack.get(DataComponents.ENTITY_DATA);
		if (data == null)
			return Optional.empty();
		EntityType<?> type = typeOf(stack);
		if (type == null || type.is(AnimalCapture.BLACKLIST))
			return Optional.empty();
		return EntityType.create(data.copyTag(), level).filter(entity -> entity instanceof Animal);
	}

	@Override
	public Component getName(ItemStack stack) {
		EntityType<?> type = typeOf(stack);
		return type == null ? super.getName(stack)
			: Component.translatable(getDescriptionId() + ".of", type.getDescription());
	}

	/** Lets the animal out onto the clicked block and gives the empty bottle back. */
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		if (!(level instanceof ServerLevel server))
			return InteractionResult.SUCCESS;
		Optional<Entity> made = create(context.getItemInHand(), server);
		if (made.isEmpty())
			return InteractionResult.FAIL;
		Entity animal = made.get();
		BlockPos at = context.getClickedPos().relative(context.getClickedFace());
		Player player = context.getPlayer();
		animal.moveTo(Vec3.atBottomCenterOf(at), player != null ? player.getYRot() + 180 : 0, 0);
		animal.setUUID(UUID.randomUUID());
		server.addFreshEntity(animal);
		server.playSound(null, at, SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1, 1);
		if (player != null && !player.getAbilities().instabuild) {
			context.getItemInHand().shrink(1);
			player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.CAPTURE_BOTTLE.get()));
		}
		return InteractionResult.CONSUME;
	}
}
