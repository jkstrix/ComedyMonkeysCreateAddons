package com.comedymonkey.createaddons.content.electric;

import java.util.Locale;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** What a Display Link reads off a Clockwork Dynamo: "768 FE a tick", "Overstressed" or "Not turning". */
public class ClockworkDynamoDisplaySource extends SingleLineDisplaySource {
	@Override
	protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
		if (!(context.getSourceBlockEntity() instanceof ClockworkDynamoBlockEntity dynamo))
			return EMPTY_LINE;
		if (!dynamo.isWorking())
			return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".display_source.clockwork_dynamo_output."
				+ (dynamo.isOverStressed() ? "overstressed" : "off"));
		return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".display_source.clockwork_dynamo_output.making",
			String.format(Locale.ROOT, "%,.0f", dynamo.outputPerTick()));
	}

	@Override
	protected boolean allowsLabeling(DisplayLinkContext context) {
		return true;
	}
}
