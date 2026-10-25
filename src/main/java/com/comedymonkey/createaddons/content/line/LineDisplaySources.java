package com.comedymonkey.createaddons.content.line;

import java.util.Locale;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.PercentOrProgressBarDisplaySource;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** What a Display Link can read off a line with a Meter on it. A line with no Meter shows nothing. */
public final class LineDisplaySources {
	private LineDisplaySources() {}

	private static LineBlockEntity meter(DisplayLinkContext context) {
		return context.getSourceBlockEntity() instanceof LineBlockEntity be && be.fitting() == Fitting.METER ? be : null;
	}

	/** "120 mB/t" or "3,200 FE/t". */
	public static class Flow extends SingleLineDisplaySource {
		@Override
		protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
			LineBlockEntity be = meter(context);
			if (be == null)
				return EMPTY_LINE;
			String unit = be instanceof FluidLineBlockEntity ? "mB/t" : "FE/t";
			return Component.literal(String.format(Locale.ROOT, "%,d %s", be.meterFlow(), unit));
		}

		@Override
		protected boolean allowsLabeling(DisplayLinkContext context) {
			return true;
		}
	}

	/** How much of the line's rate is moving, as a percentage. */
	public static class Load extends PercentOrProgressBarDisplaySource {
		@Override
		protected Float getProgress(DisplayLinkContext context) {
			LineBlockEntity be = meter(context);
			return be == null ? null : be.meterLevel();
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

	/** How much of the line's rate is moving, as a bar. */
	public static class LoadBar extends Load {
		@Override
		protected boolean progressBarActive(DisplayLinkContext context) {
			return true;
		}
	}
}
