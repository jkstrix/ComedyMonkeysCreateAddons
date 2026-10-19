package com.comedymonkey.createaddons.compat.sable;

import java.util.Locale;
import java.util.function.Function;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** One Display Link source per Flight Instruments reading, each a single labelled line. */
public class FlightDisplaySources extends SingleLineDisplaySource {
	public static final Function<FlightInstrumentsBlockEntity.Readings, String> ALTITUDE =
		r -> String.format(Locale.ROOT, "%.0f m", r.altitude());
	public static final Function<FlightInstrumentsBlockEntity.Readings, String> AIRSPEED =
		r -> String.format(Locale.ROOT, "%.1f m/s", r.airspeed());
	public static final Function<FlightInstrumentsBlockEntity.Readings, String> CLIMB =
		r -> String.format(Locale.ROOT, "%+.1f m/s", r.climb());
	public static final Function<FlightInstrumentsBlockEntity.Readings, String> HEADING =
		r -> String.format(Locale.ROOT, "%03.0f° %s", r.heading(), r.compassPoint());
	public static final Function<FlightInstrumentsBlockEntity.Readings, String> ATTITUDE =
		r -> String.format(Locale.ROOT, "P %+.0f° R %+.0f°", r.pitch(), r.roll());

	private final Function<FlightInstrumentsBlockEntity.Readings, String> reading;

	public FlightDisplaySources(Function<FlightInstrumentsBlockEntity.Readings, String> reading) {
		this.reading = reading;
	}

	@Override
	protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
		if (!(context.getSourceBlockEntity() instanceof FlightInstrumentsBlockEntity instruments))
			return EMPTY_LINE;
		return Component.literal(reading.apply(instruments.readings()));
	}

	@Override
	protected boolean allowsLabeling(DisplayLinkContext context) {
		return true;
	}

	/** Refresh every second, so a cockpit display follows the flight. */
	@Override
	public int getPassiveRefreshTicks() {
		return 20;
	}
}
