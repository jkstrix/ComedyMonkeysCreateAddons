package com.comedymonkey.createaddons.content.helperbot;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.client.helperbot.HelperBotClient;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.comedymonkey.createaddons.registry.ModDataComponents;

import java.util.List;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Helper Bot's block entity. All it keeps is his colour (D36); everything about how he moves and sounds is worked out
 * on the client, from where he is and what's around him.
 */
public class HelperBotBlockEntity extends BlockEntity implements IHaveGoggleInformation {
	/** The NBT key of his colour, also read from contraption block data. */
	public static final String DYE = "Dye";
	private static final String NAME = "CustomName";

	@Nullable
	private DyeColor dye;
	/** His anvil name, kept so he's still called it when he's broken (and shown on goggles). */
	@Nullable
	private Component name;
	/** Client only: his animation state ({@code HelperBotClient}), made the first time he's ticked or drawn. */
	@Nullable
	public Object clientState;
	/** Client only: a wave was asked for, or an order was placed at his shop. */
	public boolean waveRequested, orderPlaced;

	public HelperBotBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.HELPER_BOT.get(), pos, state);
	}

	@Nullable
	public DyeColor dye() {
		return dye;
	}

	/** His anvil name, if he has one (goggles call him by it). */
	@Nullable
	public Component name() {
		return name;
	}

	/** Goggles: what he's doing here, or what he needs to start working (D25). */
	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		(name != null ? lang().add(name.copy()) : lang().translate("gui.goggles.helper_bot")).forGoggles(tooltip);
		if (level == null)
			return true;
		Direction controls = HelperBots.controlsSide(level, worldPosition);
		BlockPos ticker = HelperBots.stockTicker(level, worldPosition);
		boolean shop = ticker != null && com.comedymonkey.createaddons.config.CMConfig.SERVER.helperBotShopKeeper.get();
		if (controls != null) {
			lang().translate("gui.goggles.helper_bot.at_controls").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
			lang().translate("gui.goggles.helper_bot.at_controls.hint").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		} else if (shop)
			lang().translate("gui.goggles.helper_bot.shop." + side(getBlockState().getValue(HelperBotBlock.FACING), worldPosition, ticker))
				.style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
		else {
			lang().translate("gui.goggles.helper_bot.off_duty").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
			lang().translate("gui.goggles.helper_bot.off_duty.hint").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		}
		if (getBlockState().getValue(HelperBotBlock.DUCKED))
			lang().translate("gui.goggles.helper_bot.ducked").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		return true;
	}

	/** Where the ticker he keeps shop for is, as he sees it: in front, behind, on his left or on his right. */
	public static String side(Direction facing, BlockPos bot, BlockPos ticker) {
		Direction to = Direction.fromDelta(ticker.getX() - bot.getX(), 0, ticker.getZ() - bot.getZ());
		return to == facing ? "front" : to == facing.getOpposite() ? "behind" : to == facing.getClockWise() ? "right" : "left";
	}

	/** A fresh builder per line: a LangBuilder keeps adding to the same text. */
	private static LangBuilder lang() {
		return new LangBuilder(com.comedymonkey.createaddons.ComedyMonkeysCreateAddons.MOD_ID);
	}

	static void clientTick(HelperBotBlockEntity be) {
		HelperBotClient.tickPlaced(be);
	}

	@Override
	public boolean triggerEvent(int id, int param) {
		if (id == HelperBotBlock.WAVE) {
			waveRequested = true;
			return true;
		}
		if (id == HelperBotBlock.ORDER) {
			orderPlaced = true;
			return true;
		}
		return super.triggerEvent(id, param);
	}

	/** His colour as it's kept in NBT (here and on contraptions); none for plain. */
	@Nullable
	public static DyeColor dyeOf(CompoundTag tag) {
		return tag.contains(DYE) ? DyeColor.byName(tag.getString(DYE), null) : null;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		if (dye != null)
			tag.putString(DYE, dye.getName());
		if (name != null)
			tag.putString(NAME, Component.Serializer.toJson(name, registries));
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		dye = dyeOf(tag);
		name = tag.contains(NAME) ? parseCustomNameSafe(tag.getString(NAME), registries) : null;
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	protected void applyImplicitComponents(DataComponentInput input) {
		super.applyImplicitComponents(input);
		dye = input.get(ModDataComponents.HELPER_BOT_DYE.get());
		name = input.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (dye != null)
			components.set(ModDataComponents.HELPER_BOT_DYE.get(), dye);
		if (name != null)
			components.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, name);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void removeComponentsFromTag(CompoundTag tag) {
		super.removeComponentsFromTag(tag);
		tag.remove(DYE);
		tag.remove(NAME);
	}
}
