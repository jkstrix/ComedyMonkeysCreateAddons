package com.comedymonkey.createaddons.content.tube;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** What a Display Link reads off a Clockwork Receiver: "120 items a minute" (design/specs/clockwork-tubes.md D34). */
public class ClockworkReceiverDisplaySource extends SingleLineDisplaySource {
	@Override
	protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
		if (!(context.getSourceBlockEntity() instanceof ClockworkReceiverBlockEntity receiver))
			return EMPTY_LINE;
		return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".display_source.clockwork_receiver_items.per_minute",
			receiver.getItemsPerMinute());
	}

	@Override
	protected boolean allowsLabeling(DisplayLinkContext context) {
		return true;
	}
}
