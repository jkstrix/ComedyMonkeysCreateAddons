package com.comedymonkey.createaddons.compat.curios.client;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.compat.curios.AviatorGogglesCurios;
import com.comedymonkey.createaddons.config.CMConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Draws the Aviator's Goggles exactly as Aeronautics draws them in the helmet slot (its armor texture on the
 * outer armor head), following the wearer's head pose.
 */
public class AviatorGogglesCurioRenderer implements ICurioRenderer {
	/** Slightly inflated head, so the goggles sit on top of a worn helmet instead of z-fighting with it. */
	public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(OVER_HELMET_LAYER,
			() -> LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1.25f), 0f), 64, 32));
	}

	/** Registers the renderer for the goggles, if Aeronautics has them; only called with Curios installed. */
	public static void register() {
		if (AviatorGogglesCurios.AVIATORS_GOGGLES.isBound())
			CuriosRendererRegistry.register(AviatorGogglesCurios.AVIATORS_GOGGLES.get(),
				() -> new AviatorGogglesCurioRenderer(Minecraft.getInstance().getEntityModels()));
	}

	public static final ModelLayerLocation OVER_HELMET_LAYER =
		new ModelLayerLocation(ComedyMonkeysCreateAddons.asResource("aviators_goggles_over_helmet"), "main");

	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
		ComedyMonkeysCreateAddons.AERONAUTICS_ID, "textures/models/armor/aviators_goggles_layer_1.png");

	private final HumanoidModel<LivingEntity> model;
	private final HumanoidModel<LivingEntity> overHelmetModel;

	public AviatorGogglesCurioRenderer(EntityModelSet models) {
		model = new HumanoidModel<>(models.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
		overHelmetModel = new HumanoidModel<>(models.bakeLayer(OVER_HELMET_LAYER));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext,
		PoseStack poseStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource buffers, int light,
		float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
		float headPitch) {

		if (!CMConfig.CLIENT.renderCurioGoggles.get())
			return;

		LivingEntity entity = slotContext.entity();
		boolean wearingHelmet = !entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
		if (wearingHelmet && !CMConfig.CLIENT.renderOverHelmet.get())
			return;

		HumanoidModel<LivingEntity> goggles = wearingHelmet ? overHelmetModel : model;

		if (renderLayerParent.getModel() instanceof HumanoidModel<?> parent) {
			((HumanoidModel<LivingEntity>) parent).copyPropertiesTo(goggles);
		} else {
			goggles.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTicks);
			goggles.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			ICurioRenderer.followHeadRotations(entity, goggles.head, goggles.hat);
		}

		goggles.setAllVisible(false);
		goggles.head.visible = true;
		goggles.hat.visible = true;

		VertexConsumer consumer = ItemRenderer.getArmorFoilBuffer(buffers, RenderType.armorCutoutNoCull(TEXTURE),
			stack.hasFoil());
		goggles.renderToBuffer(poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
	}
}
