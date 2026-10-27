package com.comedymonkey.createaddons.testutil;

import java.lang.reflect.Constructor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;

/**
 * Entity data that fails the test if anything copies it: its tag throws on {@code copy()}. {@code CustomData.of}
 * copies what it's given, so the data is built through its private constructor, holding the tag itself.
 */
public final class NoCopyEntityData {
	private NoCopyEntityData() {}

	/** Entity data holding just this id, which mustn't be copied to be read. */
	public static CustomData of(String id) {
		CompoundTag tag = new NoCopyTag();
		tag.putString("id", id);
		try {
			Constructor<CustomData> constructor = CustomData.class.getDeclaredConstructor(CompoundTag.class);
			constructor.setAccessible(true);
			return constructor.newInstance(tag);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static final class NoCopyTag extends CompoundTag {
		@Override
		public CompoundTag copy() {
			throw new AssertionError("the entity data was copied");
		}
	}
}
