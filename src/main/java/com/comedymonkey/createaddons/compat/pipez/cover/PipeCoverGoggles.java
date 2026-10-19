package com.comedymonkey.createaddons.compat.pipez.cover;

import java.util.List;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.registry.ModAttachments;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

/** What goggles (Engineer's or Aviator's) show on a covered Pipez pipe: what pipe it is, and its cover. */
public final class PipeCoverGoggles {
	private PipeCoverGoggles() {}

	public static boolean addToTooltip(BlockEntity pipe, List<Component> tooltip) {
		if (!pipe.hasData(ModAttachments.PIPE_COVER))
			return false;
		PipeCover cover = pipe.getData(ModAttachments.PIPE_COVER);
		lang().translate("gui.goggles.pipez_cover").forGoggles(tooltip);
		lang().add(pipe.getBlockState().getBlock().getName().copy()).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
		LangBuilder line = lang().translate("gui.goggles.pipez_cover." + cover.kind().getSerializedName())
			.style(ChatFormatting.GRAY);
		if (cover.hasMaterial())
			line.add(lang().text(", ").style(ChatFormatting.DARK_GRAY))
				.add(lang().add(cover.material().getBlock().getName().copy()).style(ChatFormatting.DARK_GRAY));
		line.forGoggles(tooltip, 1);
		return true;
	}

	private static LangBuilder lang() {
		return new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID);
	}
}
