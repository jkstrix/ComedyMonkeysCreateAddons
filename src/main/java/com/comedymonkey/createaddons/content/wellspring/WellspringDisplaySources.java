package com.comedymonkey.createaddons.content.wellspring;

import java.util.Locale;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.PercentOrProgressBarDisplaySource;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** What a Display Link can read off a Wellspring. */
public final class WellspringDisplaySources {
	private WellspringDisplaySources() {}

	private static MutableComponent tr(String key, Object... args) {
		return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".display_source." + key, args);
	}

	/** "Water: 12,000 / 32,000 mB", or "Empty". */
	public static class Fluid extends SingleLineDisplaySource {
		@Override
		protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
			if (!(context.getSourceBlockEntity() instanceof WellspringBlockEntity wellspring))
				return EMPTY_LINE;
			var tank = wellspring.getTank();
			if (tank.isEmpty())
				return tr("wellspring_fluid.empty");
			return tank.getFluid().getHoverName().copy()
				.append(String.format(Locale.ROOT, ": %,d / %,d mB", tank.getFluidAmount(), tank.getCapacity()));
		}

		@Override
		protected boolean allowsLabeling(DisplayLinkContext context) {
			return true;
		}
	}

	/** How full the tank is, as a percentage. */
	public static class FillPercent extends PercentOrProgressBarDisplaySource {
		@Override
		protected Float getProgress(DisplayLinkContext context) {
			if (!(context.getSourceBlockEntity() instanceof WellspringBlockEntity wellspring))
				return null;
			return wellspring.getFillPercent() / 100f;
		}

		@Override
		protected boolean progressBarActive(DisplayLinkContext context) {
			return false;
		}

		@Override
		protected boolean allowsLabeling(DisplayLinkContext context) {
			return true;
		}
	}

	/** How full the tank is, as a bar. */
	public static class FillBar extends FillPercent {
		@Override
		protected boolean progressBarActive(DisplayLinkContext context) {
			return true;
		}
	}

	/** "500 mB/s", "Full", or "No upgrades". */
	public static class Generation extends SingleLineDisplaySource {
		@Override
		protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
			if (!(context.getSourceBlockEntity() instanceof WellspringBlockEntity wellspring))
				return EMPTY_LINE;
			if (wellspring.getGenerationUpgradeCount() == 0)
				return tr("wellspring_generation.idle");
			var tank = wellspring.getTank();
			if (tank.getFluidAmount() >= tank.getCapacity())
				return tr("wellspring_generation.full");
			return Component.literal(String.format(Locale.ROOT, "%,d mB/s", wellspring.getGenerationRate()));
		}

		@Override
		protected boolean allowsLabeling(DisplayLinkContext context) {
			return true;
		}
	}
}
