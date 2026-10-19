package com.comedymonkey.createaddons.content.engine;

import java.util.Locale;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** What a Display Link can read off a Magma Engine. */
public final class MagmaEngineDisplaySources {
	private MagmaEngineDisplaySources() {}

	private static MutableComponent tr(String key, Object... args) {
		return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".display_source." + key, args);
	}

	/** "2,450 mB (61%)". */
	public static class Lava extends SingleLineDisplaySource {
		@Override
		protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
			if (!(context.getSourceBlockEntity() instanceof MagmaEngineBlockEntity engine))
				return EMPTY_LINE;
			int amount = engine.getTank().getFluidAmount();
			int percent = Math.round(100f * amount / engine.getTank().getCapacity());
			return Component.literal(String.format(Locale.ROOT, "%,d mB (%d%%)", amount, percent));
		}

		@Override
		protected boolean allowsLabeling(DisplayLinkContext context) {
			return true;
		}
	}

	/** "64 RPM, 8,192 SU", or "Out of lava". */
	public static class Output extends SingleLineDisplaySource {
		@Override
		protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
			if (!(context.getSourceBlockEntity() instanceof MagmaEngineBlockEntity engine))
				return EMPTY_LINE;
			if (engine.getGeneratedSpeed() == 0)
				return tr("magma_engine_output.off");
			float rpm = Math.abs(engine.getGeneratedSpeed());
			return Component.literal(String.format(Locale.ROOT, "%.0f RPM, %,.0f SU", rpm, rpm * engine.capacityPerRpm()));
		}

		@Override
		protected boolean allowsLabeling(DisplayLinkContext context) {
			return true;
		}
	}
}
