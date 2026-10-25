package com.comedymonkey.createaddons.content.tradebot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Every placed Trade Bot, by owner: how many each player has (D23's per-player limit), and which bots were removed
 * for good by something other than packing (a command), so their station can still be packed up without duplicating
 * anything. Saved with the world.
 */
public class TradeBotRegistry extends SavedData {
	private static final String NAME = "comedymonkeysaddons_trade_bots";

	public record Home(ResourceKey<Level> dimension, BlockPos station) {}

	private final Map<UUID, Map<UUID, Home>> byOwner = new HashMap<>();
	/** Bounded by the stations placed: an entry goes when its station is packed or removed ({@link #remove}). */
	private final Set<UUID> gone = new HashSet<>();

	public static TradeBotRegistry get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(TradeBotRegistry::new, TradeBotRegistry::load), NAME);
	}

	public int count(UUID owner) {
		Map<UUID, Home> bots = byOwner.get(owner);
		return bots == null ? 0 : bots.size();
	}

	public void add(UUID owner, UUID bot, Home home) {
		byOwner.computeIfAbsent(owner, o -> new HashMap<>()).put(bot, home);
		gone.remove(bot);
		setDirty();
	}

	/** Packed up (or his station removed): he no longer counts toward his owner's limit. */
	public void remove(UUID bot) {
		byOwner.values().forEach(bots -> bots.remove(bot));
		byOwner.values().removeIf(Map::isEmpty);
		gone.remove(bot);
		setDirty();
	}

	/** Removed for good while placed (a command): his station can be packed up on its own. */
	public void markGone(UUID bot) {
		gone.add(bot);
		setDirty();
	}

	public boolean isGone(UUID bot) {
		return gone.contains(bot);
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag owners = new ListTag();
		byOwner.forEach((owner, bots) -> bots.forEach((bot, home) -> {
			CompoundTag entry = new CompoundTag();
			entry.putUUID("Owner", owner);
			entry.putUUID("Bot", bot);
			entry.putString("Dimension", home.dimension().location().toString());
			entry.put("Station", NbtUtils.writeBlockPos(home.station()));
			owners.add(entry);
		}));
		tag.put("Bots", owners);
		ListTag goneTag = new ListTag();
		for (UUID bot : gone)
			goneTag.add(NbtUtils.createUUID(bot));
		tag.put("Gone", goneTag);
		return tag;
	}

	private static TradeBotRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
		TradeBotRegistry registry = new TradeBotRegistry();
		for (Tag t : tag.getList("Bots", Tag.TAG_COMPOUND)) {
			CompoundTag entry = (CompoundTag) t;
			ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(entry.getString("Dimension")));
			BlockPos station = NbtUtils.readBlockPos(entry, "Station").orElse(BlockPos.ZERO);
			registry.byOwner.computeIfAbsent(entry.getUUID("Owner"), o -> new HashMap<>()).put(entry.getUUID("Bot"), new Home(dimension, station));
		}
		for (Tag t : tag.getList("Gone", Tag.TAG_INT_ARRAY))
			registry.gone.add(NbtUtils.loadUUID(t));
		return registry;
	}
}
