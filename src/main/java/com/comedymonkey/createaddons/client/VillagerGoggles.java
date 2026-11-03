package com.comedymonkey.createaddons.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.VillagerWorkstations;
import com.comedymonkey.createaddons.content.Wrenches;
import com.comedymonkey.createaddons.network.WorkstationQueryPayload;
import com.comedymonkey.createaddons.network.WorkstationViewPayload;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CClient;

import net.createmod.catnip.gui.element.BoxElement;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Engineer's Goggles on villagers and their workstations ({@link VillagerWorkstations}). Look at a villager and a goggle
 * box beside the crosshair shows who it is, its workstation and how far, and on Shift its trades, drawn like the trading
 * screen. Look at a workstation and the box names the villager holding it. Holding a wrench, the other half is outlined
 * too (the villager's workstation, the workstation's villager). The server
 * knows the villager's mind, so the client asks it every half second while it looks.
 */
public final class VillagerGoggles {
	private static final int ASK_EVERY = 10;
	/** The outline, the one Create's goggles draw round a selection. */
	private static final int OUTLINE_COLOUR = 0x6FB7C4;
	private static final Object OUTLINE_KEY = new Object();
	/** One trade row: two cost slots, the arrow, the result. */
	private static final int ROW_HEIGHT = 20, COST_B_X = 22, ARROW_X = 46, RESULT_X = 60, NAME_X = 82;
	private static final ResourceLocation GOGGLES = ResourceLocation.fromNamespaceAndPath("create", "goggles");

	/** What the goggles are on: a villager's entity id, or a workstation's position. */
	private static int villager = -1;
	@Nullable
	private static BlockPos station;
	@Nullable
	private static WorkstationViewPayload view;
	private static int askIn, hoverTicks;

	private VillagerGoggles() {}

	public static void registerGui(RegisterGuiLayersEvent event) {
		event.registerAbove(VanillaGuiLayers.CROSSHAIR, ComedyMonkeysCreateAddons.asResource("villager_goggles"), VillagerGoggles::draw);
	}

	public static void accept(WorkstationViewPayload payload) {
		if (payload.aboutVillager() ? payload.villager() == villager : station != null && payload.station().filter(station::equals).isPresent())
			view = payload;
	}

	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		int lookingAt = -1;
		BlockPos lookingAtStation = null;
		if (mc.player != null && mc.level != null && !mc.player.isSpectator() && GogglesItem.isWearingGoggles(mc.player)) {
			HitResult hit = mc.hitResult;
			// (a seated villager is still what the crosshair finds: seats aren't pickable)
			if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof Villager v)
				lookingAt = v.getId();
			else if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK
				&& VillagerWorkstations.isWorkstation(mc.level.getBlockState(blockHit.getBlockPos())))
				lookingAtStation = blockHit.getBlockPos().immutable();
		}
		if (lookingAt != villager || !Objects.equals(lookingAtStation, station)) {
			villager = lookingAt;
			station = lookingAtStation;
			view = null;
			askIn = 0;
			hoverTicks = 0;
		}
		if (villager < 0 && station == null)
			return;
		hoverTicks++;
		if (--askIn <= 0) {
			askIn = ASK_EVERY;
			PacketDistributor.sendToServer(villager >= 0 ? WorkstationQueryPayload.ofVillager(villager) : WorkstationQueryPayload.ofStation(station));
		}
		WorkstationViewPayload v = view;
		if (v == null)
			return;
		// outlines only with a wrench in hand, ready to unassign
		if (!Wrenches.isWrench(mc.player.getMainHandItem()) && !Wrenches.isWrench(mc.player.getOffhandItem()))
			return;
		AABB box = null;
		if (v.aboutVillager())
			box = v.station().map(AABB::new).orElse(null);
		else if (v.villager() >= 0 && mc.level.getEntity(v.villager()) instanceof Villager owner)
			box = owner.getBoundingBox();
		if (box != null)
			Outliner.getInstance().showAABB(OUTLINE_KEY, box).colored(OUTLINE_COLOUR).lineWidth(1 / 16f);
	}

	// ---- the goggle box

	private static void draw(GuiGraphics graphics, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		WorkstationViewPayload v = view;
		if (v == null || mc.level == null || mc.player == null || mc.options.hideGui || mc.gameMode == null
			|| mc.gameMode.getPlayerMode() == GameType.SPECTATOR)
			return;
		List<Component> lines = new ArrayList<>();
		List<MerchantOffer> trades = List.of();
		if (v.aboutVillager()) {
			if (!(mc.level.getEntity(v.villager()) instanceof Villager villager))
				return;
			villagerLines(mc, villager, v, lines);
			if (!v.offers().isEmpty()) {
				if (Screen.hasShiftDown())
					trades = v.offers();
				else
					lines.add(Component.translatable("goggles.comedymonkeysaddons.villager.shift_for_trades", v.offers().size())
						.withStyle(ChatFormatting.DARK_GRAY));
			}
		} else
			stationLines(mc, v, lines);
		if (lines.isEmpty())
			return;
		// the title leaves room for the goggles drawn over its start, as Create's own boxes do
		lines.set(0, Component.literal("    ").append(lines.get(0)));

		Font font = mc.font;
		int width = 0;
		for (Component line : lines)
			width = Math.max(width, font.width(line));
		for (MerchantOffer offer : trades)
			width = Math.max(width, rowWidth(font, resultName(offer.getResult())));
		int height = 8 + (lines.size() - 1) * 10 + 2 + (trades.isEmpty() ? 0 : 4 + (trades.size() - 1) * ROW_HEIGHT + 16);

		// where Create's own goggle box goes
		int screenWidth = graphics.guiWidth(), screenHeight = graphics.guiHeight();
		CClient cfg = AllConfigs.client();
		int posX = Math.min(screenWidth / 2 + cfg.overlayOffsetX.get(), screenWidth - width - 20);
		int posY = Math.min(screenHeight / 2 + cfg.overlayOffsetY.get(), screenHeight - height - 20);
		int x = posX + 12, y = Mth.clamp(posY - 12, 4, Math.max(4, screenHeight - height - 4));

		float fade = Mth.clamp((hoverTicks + delta.getGameTimeDeltaPartialTick(false)) / 24f, 0, 1);
		boolean custom = cfg.overlayCustomColor.get();
		Color background = custom ? new Color(cfg.overlayBackgroundColor.get()) : BoxElement.COLOR_VANILLA_BACKGROUND.copy().scaleAlpha(.75f);
		Color borderTop = custom ? new Color(cfg.overlayBorderColorTop.get()) : BoxElement.COLOR_VANILLA_BORDER.getFirst().copy();
		Color borderBottom = custom ? new Color(cfg.overlayBorderColorBot.get()) : BoxElement.COLOR_VANILLA_BORDER.getSecond().copy();
		graphics.pose().pushPose();
		if (fade < 1) {
			graphics.pose().translate(Math.pow(1 - fade, 3) * Math.signum(cfg.overlayOffsetX.get() + .5f) * 8, 0, 0);
			background.scaleAlpha(fade);
			borderTop.scaleAlpha(fade);
			borderBottom.scaleAlpha(fade);
		}
		box(graphics, x, y, width, height, background.getRGB(), borderTop.getRGB(), borderBottom.getRGB());

		graphics.pose().translate(0, 0, 400);
		graphics.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(GOGGLES)), posX + 10, posY - 16);
		int lineY = y;
		for (int i = 0; i < lines.size(); i++) {
			graphics.drawString(font, lines.get(i), x, lineY, 0xFFFFFFFF);
			lineY += i == 0 ? 12 : 10;
		}
		lineY += 2;
		for (MerchantOffer offer : trades) {
			tradeRow(graphics, font, offer, x, lineY);
			lineY += ROW_HEIGHT;
		}
		graphics.pose().popPose();
	}

	private static void villagerLines(Minecraft mc, Villager villager, WorkstationViewPayload v, List<Component> lines) {
		VillagerData data = villager.getVillagerData();
		VillagerProfession profession = data.getProfession();
		MutableComponent title = villager.getDisplayName().copy();
		if (profession != VillagerProfession.NONE && profession != VillagerProfession.NITWIT)
			title.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
				.append(Component.translatable("merchant.level." + data.getLevel()).withStyle(ChatFormatting.GRAY));
		lines.add(title);
		if (profession == VillagerProfession.NITWIT) {
			lines.add(Component.translatable("goggles.comedymonkeysaddons.villager.nitwit").withStyle(ChatFormatting.GRAY));
			return;
		}
		if (villager.isBaby()) {
			lines.add(Component.translatable("goggles.comedymonkeysaddons.villager.baby").withStyle(ChatFormatting.GRAY));
			return;
		}
		if (v.station().isPresent()) {
			BlockPos pos = v.station().get();
			lines.add(Component.translatable(v.heading() ? "goggles.comedymonkeysaddons.villager.heading_to" : "goggles.comedymonkeysaddons.villager.workstation",
				stationName(mc, pos).copy().withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));
		} else if (v.claimed())
			lines.add(Component.translatable("goggles.comedymonkeysaddons.villager.elsewhere").withStyle(ChatFormatting.GRAY));
		else
			lines.add(Component.translatable("goggles.comedymonkeysaddons.villager.none").withStyle(ChatFormatting.GRAY));
		if (v.ignoringFor() > 0 && !v.claimed())
			lines.add(Component.translatable("goggles.comedymonkeysaddons.villager.ignoring", StringUtil.formatTickDuration(v.ignoringFor(), 20))
				.withStyle(ChatFormatting.DARK_GRAY));
		if (v.claimed())
			lines.add(Component.translatable("goggles.comedymonkeysaddons.villager.unassign_hint").withStyle(ChatFormatting.DARK_GRAY));
	}

	private static void stationLines(Minecraft mc, WorkstationViewPayload v, List<Component> lines) {
		BlockPos pos = v.station().orElse(null);
		if (pos == null)
			return;
		lines.add(stationName(mc, pos));
		Entity owner = v.villager() >= 0 ? mc.level.getEntity(v.villager()) : null;
		if (owner != null)
			lines.add(Component.translatable(v.heading() ? "goggles.comedymonkeysaddons.station.reserved_by" : "goggles.comedymonkeysaddons.station.claimed_by",
				owner.getDisplayName().copy().withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));
		else
			lines.add(Component.translatable(v.claimed() ? "goggles.comedymonkeysaddons.station.claimed_unseen" : "goggles.comedymonkeysaddons.station.free")
				.withStyle(ChatFormatting.GRAY));
	}

	private static Component stationName(Minecraft mc, BlockPos pos) {
		return mc.level.isLoaded(pos) && VillagerWorkstations.isWorkstation(mc.level.getBlockState(pos)) ? mc.level.getBlockState(pos).getBlock().getName()
			: Component.translatable("message.comedymonkeysaddons.villager.workstation");
	}

	/** A trade as the trading screen shows it: costs, an arrow, the result and its enchantment. A used-up trade is greyed out. */
	private static void tradeRow(GuiGraphics graphics, Font font, MerchantOffer offer, int x, int y) {
		ItemStack costA = offer.getCostA(), costB = offer.getCostB(), result = offer.getResult();
		item(graphics, font, costA, x, y);
		if (!costB.isEmpty())
			item(graphics, font, costB, x + COST_B_X, y);
		graphics.drawString(font, "→", x + ARROW_X, y + 4, 0xFFAAAAAA);
		item(graphics, font, result, x + RESULT_X, y);
		Component name = resultName(result);
		if (name != null)
			graphics.drawString(font, name, x + NAME_X, y + 4, 0xFFAAAAAA);
		if (offer.isOutOfStock()) {
			graphics.pose().pushPose();
			graphics.pose().translate(0, 0, 250);
			graphics.fill(x - 1, y - 1, x + rowWidth(font, name) + 1, y + 17, 0xB0100010);
			graphics.pose().popPose();
		}
	}

	private static int rowWidth(Font font, @Nullable Component name) {
		return name == null ? RESULT_X + 16 : NAME_X + font.width(name);
	}

	private static void item(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
		graphics.renderItem(stack, x, y);
		graphics.renderItemDecorations(font, stack, x, y);
	}

	/** The result's enchantment (a book's, or gear's), "and N more" if it has several; null for anything else. */
	@Nullable
	private static Component resultName(ItemStack result) {
		ItemEnchantments enchantments = result.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
		if (enchantments.isEmpty())
			enchantments = result.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
		if (enchantments.isEmpty())
			return null;
		var first = enchantments.entrySet().iterator().next();
		Holder<Enchantment> enchantment = first.getKey();
		MutableComponent name = Enchantment.getFullname(enchantment, first.getIntValue()).copy();
		if (enchantments.size() > 1)
			name.append(Component.translatable("goggles.comedymonkeysaddons.villager.more_enchantments", enchantments.size() - 1));
		return name;
	}

	/** Create's goggle box (its {@code RemovedGuiUtils.drawHoveringText} frame) round {@code width} x {@code height} at x, y. */
	private static void box(GuiGraphics g, int x, int y, int width, int height, int background, int borderStart, int borderEnd) {
		int z = 400;
		g.fillGradient(x - 3, y - 4, x + width + 3, y - 3, z, background, background);
		g.fillGradient(x - 3, y + height + 3, x + width + 3, y + height + 4, z, background, background);
		g.fillGradient(x - 3, y - 3, x + width + 3, y + height + 3, z, background, background);
		g.fillGradient(x - 4, y - 3, x - 3, y + height + 3, z, background, background);
		g.fillGradient(x + width + 3, y - 3, x + width + 4, y + height + 3, z, background, background);
		g.fillGradient(x - 3, y - 2, x - 2, y + height + 2, z, borderStart, borderEnd);
		g.fillGradient(x + width + 2, y - 2, x + width + 3, y + height + 2, z, borderStart, borderEnd);
		g.fillGradient(x - 3, y - 3, x + width + 3, y - 2, z, borderStart, borderStart);
		g.fillGradient(x - 3, y + height + 2, x + width + 3, y + height + 3, z, borderEnd, borderEnd);
	}
}
