package com.comedymonkey.createaddons.client;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.pen.BottledAnimalItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * A Bottled Animal: the Capture Bottle's jar with the animal's own model standing inside it, small enough to fit.
 * The animal is drawn first and the glass over it, so it shows through. Animals are made once per bottle and kept.
 */
public class BottledAnimalRenderer extends BlockEntityWithoutLevelRenderer {
	private static final ModelResourceLocation JAR = ModelResourceLocation.inventory(ComedyMonkeysCreateAddons.asResource("capture_bottle"));
	/** Room inside the jar, in blocks: it's 8 pixels across and 9 high, so this leaves a margin all round. */
	private static final float ROOM = 5.5f / 16, FLOOR = 0.3f / 16;
	/** Animals are longer than their hitbox is wide (a sheep's or cow's model is about this many times as long). */
	private static final float LENGTH_PER_WIDTH = 1.7f;
	/** Turned so its side shows at the angle items are seen at in the inventory. */
	private static final float FACING = 135;

	private final Map<String, Entity> animals = new LinkedHashMap<>(16, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Entity> eldest) {
			return size() > 64;
		}
	};
	@Nullable
	private Level animalsFor;

	public BottledAnimalRenderer() {
		super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack ms, MultiBufferSource buffer, int light,
							 int overlay) {
		Minecraft mc = Minecraft.getInstance();
		Entity animal = animalFor(stack, mc.level);
		if (animal != null) {
			float scale = ROOM / Math.max(animal.getBbHeight(), animal.getBbWidth() * LENGTH_PER_WIDTH);
			ms.pushPose();
			ms.translate(0.5, FLOOR, 0.5);
			ms.scale(scale, scale, scale);
			EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
			dispatcher.setRenderShadow(false);
			try {
				dispatcher.render(animal, 0, 0, 0, FACING, 0, ms, buffer, light);
			} finally {
				dispatcher.setRenderShadow(true);
			}
			ms.popPose();
		}
		// The jar over it
		BakedModel jar = mc.getModelManager().getModel(JAR);
		for (RenderType type : jar.getRenderTypes(stack, true))
			mc.getItemRenderer().renderModelLists(jar, stack, light, overlay, ms,
				ItemRenderer.getFoilBufferDirect(buffer, type, true, stack.hasFoil()));
	}

	/** The animal in this bottle, made once and kept (per world, as entities belong to one). */
	@Nullable
	private Entity animalFor(ItemStack stack, @Nullable Level level) {
		if (level == null)
			return null;
		if (level != animalsFor) {
			animals.clear();
			animalsFor = level;
		}
		CustomData data = stack.get(DataComponents.ENTITY_DATA);
		if (data == null)
			return null;
		String key = data.copyTag().toString();
		Entity animal = animals.get(key);
		if (animal == null && !animals.containsKey(key)) {
			animal = BottledAnimalItem.create(stack, level).orElse(null);
			if (animal instanceof LivingEntity living) {
				living.yBodyRot = living.yBodyRotO = living.yHeadRot = living.yHeadRotO = FACING;
				living.setYRot(FACING);
				living.yRotO = FACING;
			}
			animals.put(key, animal);
		}
		return animal;
	}
}
