package com.comedymonkey.createaddons.content.tradebot;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipLibrary;
import com.comedymonkey.createaddons.registry.ModEntities;
import com.comedymonkey.createaddons.registry.ModItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

/**
 * Which robot a {@link TradeBotEntity} is: Trade Bot, or the Trade Master built on him (design/plans/trade-master.md).
 * They share the body, the motion and the clips' timings; each has his own model, texture and clip file, exported by
 * {@code trade-bot.gen.js} (the Trade Master's with {@code --variant=trade-master.variant.js}).
 * <p>
 * The Trade Master's clip file holds Trade Bot's clips in the same order, with the same lengths, modes, root tracks and
 * cues (TradeMasterClipsTest), plus tracks for his own parts (beard, monocle, chain). So the server times and moves
 * both with Trade Bot's library, and only the client, posing him, reads his own ({@link #own}).
 */
public enum BotKind {
	TRADE_BOT("trade_bot"),
	TRADE_MASTER("trade_master");

	private final String id;

	BotKind(String id) {
		this.id = id;
	}

	/** The id his entity type, assets and lang keys use. */
	public String id() {
		return id;
	}

	public String clipsPath() {
		return "/assets/" + ComedyMonkeysCreateAddons.MOD_ID + "/" + id + "/clips.json";
	}

	public String geometryPath() {
		return "/assets/" + ComedyMonkeysCreateAddons.MOD_ID + "/" + id + "/geometry.json";
	}

	public ResourceLocation texture() {
		return ComedyMonkeysCreateAddons.asResource("textures/entity/" + id + ".png");
	}

	public ResourceLocation glowTexture() {
		return ComedyMonkeysCreateAddons.asResource("textures/entity/" + id + "_glow.png");
	}

	public EntityType<TradeBotEntity> entityType() {
		return this == TRADE_MASTER ? ModEntities.TRADE_MASTER.get() : ModEntities.TRADE_BOT.get();
	}

	/** The item he packs into, on his station. */
	public Item item() {
		return this == TRADE_MASTER ? ModItems.TRADE_MASTER.get() : ModItems.TRADE_BOT.get();
	}

	/** How many of this kind each player may have placed (Trade Masters are counted apart, D29). */
	public int perPlayer() {
		return this == TRADE_MASTER ? CMConfig.SERVER.tradeMastersPerPlayer() : CMConfig.SERVER.tradeBotsPerPlayer();
	}

	/** The kind with this id; Trade Bot for anything else (old saves have none). */
	public static BotKind byId(String id) {
		for (BotKind kind : values())
			if (kind.id.equals(id))
				return kind;
		return TRADE_BOT;
	}

	public ClipLibrary clips() {
		return ClipLibrary.of(clipsPath());
	}

	/** His own copy of a clip (the same timings, with his own parts' tracks); the clip itself if he has none. */
	public Clip own(@Nullable Clip clip) {
		if (clip == null || this == TRADE_BOT)
			return clip;
		Clip own = clips().find(clip.name());
		return own != null ? own : clip;
	}
}
