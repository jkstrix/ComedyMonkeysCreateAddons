package com.comedymonkey.createaddons.content.cage;

import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Iron Jar and Netherite Jar: catch a monster for a Monster Cage (see {@link MonsterCapture}). One item each, empty or
 * holding a monster in the stack's entity data, like the Capture Bottle and its Bottled Animal in one. Empty jars stack
 * to 16, a filled one to 1. The Iron Jar takes any monster a cage takes but the Wither; the Netherite Jar only the
 * Wither, which it can only let out into a Netherite cage (D21). The jar keeps everything about its monster but its
 * place, its id, its lead and its gear (D27).
 */
public class MonsterJarItem extends Item {
	private final boolean netherite;

	public MonsterJarItem(Properties properties, boolean netherite) {
		super(properties);
		this.netherite = netherite;
	}

	public boolean isNetherite() {
		return netherite;
	}

	public static boolean isFilled(ItemStack stack) {
		return stack.getItem() instanceof MonsterJarItem && stack.has(DataComponents.ENTITY_DATA);
	}

	/** Whether this jar is the kind that holds this monster (the Netherite Jar for the Wither, else the Iron Jar). */
	public boolean holds(EntityType<?> type) {
		CageRules.Kind kind = CageRules.kindOf(type);
		return kind != null && (kind == CageRules.Kind.WITHER) == netherite;
	}

	/** A jar of this kind holding the monster; the monster is removed from the world by the caller. */
	public ItemStack fill(Entity monster) {
		CompoundTag tag = new CompoundTag();
		monster.saveWithoutId(tag);
		tag.putString("id", EntityType.getKey(monster.getType()).toString());
		for (String key : new String[] {"Pos", "Motion", "Rotation", "UUID", "leash", "Leash", "FallDistance", "Fire",
			"OnGround", "ArmorItems", "HandItems", "body_armor_item", "ArmorDropChances", "HandDropChances"})
			tag.remove(key);
		ItemStack stack = new ItemStack(this);
		stack.set(DataComponents.ENTITY_DATA, CustomData.of(tag));
		stack.set(DataComponents.MAX_STACK_SIZE, 1);
		return stack;
	}

	/** The empty jar a filled one came in. */
	public ItemStack empty() {
		return new ItemStack(this);
	}

	@Nullable
	@SuppressWarnings("deprecation") // getUnsafe: read, never changed
	public static EntityType<?> typeOf(ItemStack stack) {
		CustomData data = stack.get(DataComponents.ENTITY_DATA);
		if (data == null)
			return null;
		// read in place: a copy of the whole monster just for its id was the cages' busiest call
		return EntityType.byString(data.getUnsafe().getString("id")).orElse(null);
	}

	/**
	 * The monster in the jar as a new entity (not in any world), or empty. Only a monster a cage could take comes out:
	 * a stack edited to hold anything else makes nothing.
	 */
	public static Optional<Entity> create(ItemStack stack, Level level) {
		CustomData data = stack.get(DataComponents.ENTITY_DATA);
		EntityType<?> type = typeOf(stack);
		if (data == null || type == null || CageRules.kindOf(type) == null)
			return Optional.empty();
		return EntityType.create(data.copyTag(), level).filter(CageRules::isCageable);
	}

	@Override
	public Component getName(ItemStack stack) {
		EntityType<?> type = typeOf(stack);
		return type == null ? super.getName(stack) : Component.translatable(getDescriptionId() + ".of", type.getDescription());
	}

	/** A filled Iron Jar lets its monster out onto the clicked block and gives the empty jar back (never on Peaceful). */
	@Override
	public InteractionResult useOn(UseOnContext context) {
		ItemStack stack = context.getItemInHand();
		if (!isFilled(stack))
			return InteractionResult.PASS;
		Level level = context.getLevel();
		Player player = context.getPlayer();
		if (netherite) {
			if (player != null && !level.isClientSide)
				player.displayClientMessage(Component.translatable("comedymonkeysaddons.gui.monster_cage.wither_only_cage"), true);
			return InteractionResult.FAIL;
		}
		if (!(level instanceof ServerLevel server))
			return InteractionResult.SUCCESS;
		if (server.getDifficulty() == Difficulty.PEACEFUL) {
			if (player != null)
				player.displayClientMessage(Component.translatable("comedymonkeysaddons.gui.monster_cage.peaceful"), true);
			return InteractionResult.FAIL;
		}
		Optional<Entity> made = create(stack, server);
		if (made.isEmpty())
			return InteractionResult.FAIL;
		BlockPos at = context.getClickedPos().relative(context.getClickedFace());
		release(server, made.get(), Vec3.atBottomCenterOf(at), player != null ? player.getYRot() + 180 : 0);
		if (player != null && !player.getAbilities().instabuild) {
			stack.shrink(1);
			player.getInventory().placeItemBackInInventory(empty());
		}
		return InteractionResult.CONSUME;
	}

	/** Puts a jar's monster into the world (also for Dispensers and Deployers). */
	public static void release(ServerLevel level, Entity monster, Vec3 at, float yaw) {
		monster.moveTo(at, yaw, 0);
		monster.setUUID(UUID.randomUUID());
		level.addFreshEntity(monster);
		level.playSound(null, BlockPos.containing(at), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1, 0.8f);
		level.playSound(null, BlockPos.containing(at), ModSounds.cage("jar_out"), SoundSource.PLAYERS, 1, 1);
	}
}
