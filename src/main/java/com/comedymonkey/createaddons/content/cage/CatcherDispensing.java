package com.comedymonkey.createaddons.content.cage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.comedymonkey.createaddons.content.pen.AnimalCapture;
import com.comedymonkey.createaddons.content.pen.BottledAnimalItem;
import com.comedymonkey.createaddons.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Dispensers catch and release with the jars and the Capture Bottle (D48, D50, D64), into and out of the block in front:
 * an empty one catches what it could catch by hand, a full one lets its creature out there (never a Wither, never a
 * monster on Peaceful, never where its body doesn't fit). The full or empty container swaps in for the one used, into the
 * dispenser if there's room, else out of its front (always, on a contraption). Deployers need nothing here: they use the items as a player does.
 */
public final class CatcherDispensing {
	private static final DefaultDispenseItemBehavior DROP = new DefaultDispenseItemBehavior();

	private CatcherDispensing() {}

	/** From common setup. */
	public static void register() {
		DispenserBlock.registerBehavior(ModItems.CAPTURE_BOTTLE.get(), new Behaviour() {
			@Override
			ItemStack use(ServerLevel level, BlockPos front, Direction facing, ItemStack stack) {
				Entity animal = first(level, front, entity -> AnimalCapture.canCapture(null, entity));
				return animal == null ? ItemStack.EMPTY : AnimalCapture.bottle(level, animal);
			}
		});
		DispenserBlock.registerBehavior(ModItems.BOTTLED_ANIMAL.get(), new Behaviour() {
			@Override
			ItemStack use(ServerLevel level, BlockPos front, Direction facing, ItemStack stack) {
				Optional<Entity> made = BottledAnimalItem.create(stack, level);
				if (made.isEmpty())
					return ItemStack.EMPTY;
				Entity animal = made.get();
				if (!fits(level, animal, front, facing))
					return ItemStack.EMPTY;
				animal.setUUID(UUID.randomUUID());
				level.addFreshEntity(animal);
				level.playSound(null, front, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1, 1);
				return new ItemStack(ModItems.CAPTURE_BOTTLE.get());
			}
		});
		for (var item : List.of(ModItems.IRON_JAR, ModItems.NETHERITE_JAR))
			DispenserBlock.registerBehavior(item.get(), new Behaviour() {
				@Override
				ItemStack use(ServerLevel level, BlockPos front, Direction facing, ItemStack stack) {
					MonsterJarItem jar = (MonsterJarItem) stack.getItem();
					if (!MonsterJarItem.isFilled(stack)) {
						Entity monster = first(level, front, entity -> MonsterCapture.canCatch(jar, entity));
						return monster == null ? ItemStack.EMPTY : MonsterCapture.jar(level, jar, monster);
					}
					// a Wither only ever goes into a Netherite cage (D50), and nothing comes out on Peaceful
					if (jar.isNetherite() || level.getDifficulty() == Difficulty.PEACEFUL)
						return ItemStack.EMPTY;
					Optional<Entity> made = MonsterJarItem.create(stack, level);
					if (made.isEmpty() || !fits(level, made.get(), front, facing))
						return ItemStack.EMPTY;
					MonsterJarItem.release(level, made.get(), Vec3.atBottomCenterOf(front), facing.toYRot());
					return jar.empty();
				}
			});
	}

	/** Catch-or-release: {@link #use} gives what the used container becomes, or empty if nothing happened. */
	private abstract static class Behaviour extends OptionalDispenseItemBehavior {
		abstract ItemStack use(ServerLevel level, BlockPos front, Direction facing, ItemStack stack);

		@Override
		protected ItemStack execute(BlockSource source, ItemStack stack) {
			Direction facing = source.state().getValue(DispenserBlock.FACING);
			ItemStack result = use(source.level(), source.pos().relative(facing), facing, stack);
			setSuccess(!result.isEmpty());
			return result.isEmpty() ? stack : swap(source, stack, result);
		}
	}

	/** The first living thing in the block that {@code test} takes. */
	private static Entity first(ServerLevel level, BlockPos front, java.util.function.Predicate<Entity> test) {
		List<Entity> found = level.getEntitiesOfClass(Entity.class, new AABB(front), entity -> entity.isAlive() && test.test(entity));
		return found.isEmpty() ? null : found.get(0);
	}

	/** Stands it in the block in front, facing out; whether its whole body has room there (no glass, nothing overhead). */
	private static boolean fits(ServerLevel level, Entity entity, BlockPos front, Direction facing) {
		entity.moveTo(Vec3.atBottomCenterOf(front), facing.toYRot(), 0);
		return level.noCollision(entity);
	}

	/**
	 * One of the dispensed stack becomes {@code result}: in its place if it was the last, else into the dispenser or out
	 * of its front. A Dispenser on a Create contraption has no block entity here, so its result always goes out the front.
	 */
	private static ItemStack swap(BlockSource source, ItemStack stack, ItemStack result) {
		stack.shrink(1);
		if (stack.isEmpty())
			return result;
		ItemStack rest = source.blockEntity() == null ? result : source.blockEntity().insertItem(result);
		if (!rest.isEmpty())
			DROP.dispense(source, rest);
		return stack;
	}
}
