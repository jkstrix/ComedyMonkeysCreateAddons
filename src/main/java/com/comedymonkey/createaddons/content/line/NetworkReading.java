package com.comedymonkey.createaddons.content.line;

import java.util.List;
import java.util.Locale;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * What a block on a Power Line shows on the goggles about its network: what the network's generators make flat out
 * and what its machines are using, FE a tick. The network sets it once a second; clients are told when it changes
 * noticeably (and now and then, so it's known to be fresh), and stop showing it once it's a few seconds old.
 */
public final class NetworkReading {
	/** A block that shows it (generators, motors, the wires that meet them). */
	public interface Shown {
		void networkReading(int makes, int uses);
	}

	/** Ticks after its last update it's still shown; the network updates once a second. */
	private static final int FRESH = 60;

	private int makes, uses;
	private long at = Long.MIN_VALUE / 2, sentAt = Long.MIN_VALUE / 2;
	private int sentMakes = -1, sentUses = -1;

	/** Server: the network's figures now; true when clients should be told. */
	public boolean set(int makes, int uses, long now) {
		this.makes = makes;
		this.uses = uses;
		at = now;
		boolean tell = far(makes, sentMakes) || far(uses, sentUses) || now - sentAt >= FRESH / 2;
		if (tell) {
			sentMakes = makes;
			sentUses = uses;
			sentAt = now;
		}
		return tell;
	}

	private static boolean far(int a, int b) {
		return Math.abs(a - b) > Math.max(16, Math.max(Math.abs(a), Math.abs(b)) / 50);
	}

	public void write(CompoundTag tag) {
		tag.putInt("NetMakes", makes);
		tag.putInt("NetUses", uses);
		tag.putLong("NetAt", at);
	}

	public void read(CompoundTag tag) {
		makes = tag.getInt("NetMakes");
		uses = tag.getInt("NetUses");
		at = tag.contains("NetAt") ? tag.getLong("NetAt") : Long.MIN_VALUE / 2;
	}

	/** The goggles' line, while the figures are fresh. */
	public void addTo(List<Component> tooltip, Level level, int indent) {
		if (level == null || level.getGameTime() - at > FRESH)
			return;
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID)
			.translate("gui.goggles.network", String.format(Locale.ROOT, "%,d", makes), String.format(Locale.ROOT, "%,d", uses))
			.style(ChatFormatting.GRAY).forGoggles(tooltip, indent);
	}
}
