package com.comedymonkey.createaddons.client;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;

/**
 * A sprite on the block atlas, looked up by name once rather than every frame, and again only after the atlas has
 * been stitched anew (a resource reload). Only used on the render thread. Sodium only animates the sprites it sees in
 * use, which it learns from atlas lookups; without the lookup each frame, each use tells it instead.
 */
public final class BlockSprite {
	/** How many times the block atlas has been stitched: a sprite looked up before the last one is stale. */
	private static int stitches;
	/** For sprites picked at run time, one per name. */
	private static final Map<ResourceLocation, BlockSprite> BY_NAME = new HashMap<>();
	/** Sodium's "this sprite is in use" (or Embeddium's), if either is installed. */
	@Nullable
	private static final MethodHandle MARK_ACTIVE = markActive();

	private final ResourceLocation id;
	private TextureAtlasSprite sprite;
	private int seen = -1;

	public BlockSprite(ResourceLocation id) {
		this.id = id;
	}

	public TextureAtlasSprite get() {
		if (sprite == null || seen != stitches) {
			sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(id);
			seen = stitches;
		}
		if (MARK_ACTIVE != null)
			try {
				MARK_ACTIVE.invoke(sprite);
			} catch (Throwable e) {
				// it only keeps the sprite animating
			}
		return sprite;
	}

	/** The sprite with this name, for names only known at run time. */
	public static TextureAtlasSprite of(ResourceLocation id) {
		return BY_NAME.computeIfAbsent(id, BlockSprite::new).get();
	}

	/** Sodium's SpriteUtil.INSTANCE.markSpriteActive, or Embeddium's static SpriteUtil.markSpriteActive, bound and ready. */
	@Nullable
	private static MethodHandle markActive() {
		MethodType type = MethodType.methodType(void.class, TextureAtlasSprite.class);
		try {
			if (ModList.get().isLoaded("sodium")) {
				Class<?> util = Class.forName("net.caffeinemc.mods.sodium.api.texture.SpriteUtil");
				Object instance = util.getField("INSTANCE").get(null);
				return MethodHandles.publicLookup().findVirtual(util, "markSpriteActive", type).bindTo(instance);
			}
			if (ModList.get().isLoaded("embeddium")) {
				Class<?> util = Class.forName("org.embeddedt.embeddium.api.render.texture.SpriteUtil");
				return MethodHandles.publicLookup().findStatic(util, "markSpriteActive", type);
			}
		} catch (ReflectiveOperationException | LinkageError e) {
			// not the version we know: sprites are left to it
		}
		return null;
	}

	/** On the mod bus: the block atlas has been stitched, so every sprite is looked up again. */
	public static void onStitched(TextureAtlasStitchedEvent event) {
		if (event.getAtlas().location().equals(InventoryMenu.BLOCK_ATLAS))
			stitches++;
	}
}
