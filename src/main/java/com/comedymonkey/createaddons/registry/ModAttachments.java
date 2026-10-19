package com.comedymonkey.createaddons.registry;

import java.util.function.Supplier;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.compat.pipez.cover.PipeCover;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Data we keep on other mods' block entities. */
public final class ModAttachments {
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
		DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ComedyMonkeysCreateAddons.MOD_ID);

	/** The cover on a Pipez pipe. Saved with the pipe; sent to clients by {@code PipeCoverPayload}. */
	public static final Supplier<AttachmentType<PipeCover>> PIPE_COVER = ATTACHMENT_TYPES.register("pipe_cover",
		() -> AttachmentType.builder(() -> PipeCover.NONE).serialize(PipeCover.CODEC).build());

	private ModAttachments() {}
}
