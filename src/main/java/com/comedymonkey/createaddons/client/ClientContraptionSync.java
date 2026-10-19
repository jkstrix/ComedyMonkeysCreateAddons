package com.comedymonkey.createaddons.client;

import com.comedymonkey.createaddons.network.ContraptionBlockSyncPayload;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/** Client side of {@link ContraptionBlockSyncPayload}: store the new block and rebuild the contraption's render. */
public final class ClientContraptionSync {
	private ClientContraptionSync() {}

	public static void handle(ContraptionBlockSyncPayload payload) {
		if (Minecraft.getInstance().level == null
			|| !(Minecraft.getInstance().level.getEntity(payload.entityId()) instanceof AbstractContraptionEntity entity))
			return;
		var contraption = entity.getContraption();
		if (contraption == null || !contraption.getBlocks().containsKey(payload.localPos()))
			return;
		contraption.getBlocks().put(payload.localPos(),
			new StructureBlockInfo(payload.localPos(), payload.state(), payload.nbt()));
		contraption.resetClientContraption();
	}
}
