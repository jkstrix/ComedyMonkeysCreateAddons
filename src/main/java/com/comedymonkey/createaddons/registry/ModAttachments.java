package com.comedymonkey.createaddons.registry;

import java.util.function.Supplier;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.compat.pipez.cover.PipeCover;
import com.comedymonkey.createaddons.content.clock.ClockCheck;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Data we keep on other mods' block entities, and on vanilla mobs. */
public final class ModAttachments {
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
		DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ComedyMonkeysCreateAddons.MOD_ID);

	/** The cover on a Pipez pipe. Saved with the pipe; sent to clients by {@code PipeCoverPayload}. */
	public static final Supplier<AttachmentType<PipeCover>> PIPE_COVER = ATTACHMENT_TYPES.register("pipe_cover",
		() -> AttachmentType.builder(() -> PipeCover.NONE).serialize(PipeCover.CODEC).build());

	/** On villagers: when they last asked a Cuckoo Clock whether to restock. Saved with them. */
	public static final Supplier<AttachmentType<ClockCheck>> CLOCK_RESTOCK_CHECK = ATTACHMENT_TYPES.register("clock_check",
		() -> AttachmentType.builder(() -> ClockCheck.NEVER).serialize(ClockCheck.CODEC).build());

	private ModAttachments() {}
}
