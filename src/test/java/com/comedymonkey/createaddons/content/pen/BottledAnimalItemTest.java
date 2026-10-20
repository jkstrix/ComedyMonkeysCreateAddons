package com.comedymonkey.createaddons.content.pen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.registry.ModItems;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

class BottledAnimalItemTest {
	private static HolderLookup.Provider registries;

	@BeforeAll
	static void setUp() {
		registries = VanillaRegistries.createLookup();
	}

	private static ItemStack bottleWith(CompoundTag tag) {
		ItemStack stack = new ItemStack(ModItems.BOTTLED_ANIMAL.get());
		stack.set(DataComponents.ENTITY_DATA, CustomData.of(tag));
		return stack;
	}

	private static ItemStack bottleOf(String id) {
		CompoundTag tag = new CompoundTag();
		tag.putString("id", id);
		tag.putString("CustomName", "\"Daisy\"");
		tag.putInt("Age", -1200);
		return bottleWith(tag);
	}

	@Test
	void anEmptyBottleHoldsNothing() {
		assertNull(BottledAnimalItem.typeOf(new ItemStack(ModItems.BOTTLED_ANIMAL.get())));
		assertNull(BottledAnimalItem.typeOf(ItemStack.EMPTY));
	}

	@Test
	void knowsWhatItHolds() {
		assertSame(EntityType.COW, BottledAnimalItem.typeOf(bottleOf("minecraft:cow")));
		assertSame(EntityType.SHEEP, BottledAnimalItem.typeOf(bottleOf("minecraft:sheep")));
	}

	@Test
	void anUnknownAnimalIsNothing() {
		// Say from a mod that has since been removed
		assertNull(BottledAnimalItem.typeOf(bottleOf("somemod:capybara")));
		assertNull(BottledAnimalItem.typeOf(bottleWith(new CompoundTag())));
	}

	@Test
	void aMalformedIdIsNothing() {
		assertNull(BottledAnimalItem.typeOf(bottleOf("Not A Mob!")));
	}

	@Test
	void namedAfterItsAnimal() {
		Component name = ModItems.BOTTLED_ANIMAL.get().getName(bottleOf("minecraft:cow"));
		TranslatableContents contents = assertInstanceOf(TranslatableContents.class, name.getContents());
		assertEquals("item.comedymonkeysaddons.bottled_animal.of", contents.getKey());
		assertEquals(1, contents.getArgs().length);
		assertEquals(EntityType.COW.getDescription(), contents.getArgs()[0]);
	}

	@Test
	void plainNameWhenEmpty() {
		Component name = ModItems.BOTTLED_ANIMAL.get().getName(new ItemStack(ModItems.BOTTLED_ANIMAL.get()));
		TranslatableContents contents = assertInstanceOf(TranslatableContents.class, name.getContents());
		assertEquals("item.comedymonkeysaddons.bottled_animal", contents.getKey());
	}

	@Test
	void oneAnimalABottle() {
		assertEquals(1, new ItemStack(ModItems.BOTTLED_ANIMAL.get()).getMaxStackSize());
	}

	@Test
	void keepsTheWholeAnimalWhenSaved() {
		ItemStack bottle = bottleOf("minecraft:sheep");
		Tag saved = bottle.save(registries);
		ItemStack loaded = ItemStack.parseOptional(registries, (CompoundTag) saved);
		assertSame(ModItems.BOTTLED_ANIMAL.get(), loaded.getItem());
		assertSame(EntityType.SHEEP, BottledAnimalItem.typeOf(loaded));
		CompoundTag data = loaded.get(DataComponents.ENTITY_DATA).copyTag();
		assertEquals("\"Daisy\"", data.getString("CustomName"));
		assertEquals(-1200, data.getInt("Age"));
		assertTrue(ItemStack.isSameItemSameComponents(bottle, loaded));
	}

	/** Why {@link BottledAnimalItem#of} writes the "id": entity data without one can't be saved at all. */
	@Test
	void entityDataNeedsAnId() {
		CompoundTag noId = new CompoundTag();
		noId.putInt("Age", 0);
		var result = ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), bottleWith(noId));
		assertTrue(result.error().isPresent());
	}
}
