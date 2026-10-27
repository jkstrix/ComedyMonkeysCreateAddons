package com.comedymonkey.createaddons.client;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.cage.MonsterJarItem;
import com.google.common.collect.MapMaker;
import com.mojang.blaze3d.vertex.PoseStack;

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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * The Iron and Netherite Jars: the jar's own model (built from the Capture Bottle's), with the monster standing inside
 * it, small enough to fit, when it holds one. The monster is drawn first and the glass over it. Monsters are made once
 * per jar and kept, as the Bottled Animal's are.
 */
public class MonsterJarRenderer extends BlockEntityWithoutLevelRenderer {
	public static final ModelResourceLocation IRON = ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("item/iron_jar_model"));
	public static final ModelResourceLocation NETHERITE = ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("item/netherite_jar_model"));
	/** Room inside: the glass is 9 px across and 8 high. */
	private static final float ROOM = 5.5f / 16, FLOOR = 0.3f / 16, FACING = 135;
	private static final Map<CustomData, Entity> MONSTERS = new MapMaker().weakKeys().makeMap();
	private static final Map<CustomData, Boolean> NONE = new MapMaker().weakKeys().makeMap();
	@Nullable
	private static Level monstersFor;

	public MonsterJarRenderer() {
		super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		Minecraft mc = Minecraft.getInstance();
		Entity monster = monsterFor(stack, mc.level);
		if (monster != null) {
			float scale = ROOM / Math.max(monster.getBbHeight(), monster.getBbWidth() * 1.4f);
			ms.pushPose();
			ms.translate(0.5, FLOOR, 0.5);
			ms.scale(scale, scale, scale);
			EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
			dispatcher.setRenderShadow(false);
			try {
				dispatcher.render(monster, 0, 0, 0, FACING, 0, ms, buffer, light);
			} finally {
				dispatcher.setRenderShadow(true);
			}
			ms.popPose();
		}
		boolean netherite = stack.getItem() instanceof MonsterJarItem jar && jar.isNetherite();
		BakedModel model = mc.getModelManager().getModel(netherite ? NETHERITE : IRON);
		for (RenderType type : model.getRenderTypes(stack, true))
			mc.getItemRenderer().renderModelLists(model, stack, light, overlay, ms,
				ItemRenderer.getFoilBufferDirect(buffer, type, true, stack.hasFoil()));
	}

	public static void forget() {
		MONSTERS.clear();
		NONE.clear();
		monstersFor = null;
	}

	@Nullable
	private static Entity monsterFor(ItemStack stack, @Nullable Level level) {
		if (level == null)
			return null;
		if (level != monstersFor) {
			forget();
			monstersFor = level;
		}
		CustomData data = stack.get(DataComponents.ENTITY_DATA);
		if (data == null || NONE.containsKey(data))
			return null;
		Entity monster = MONSTERS.get(data);
		if (monster == null) {
			monster = MonsterJarItem.create(stack, level).orElse(null);
			if (monster == null) {
				NONE.put(data, Boolean.TRUE);
				return null;
			}
			if (monster instanceof Mob mob)
				for (EquipmentSlot slot : EquipmentSlot.values())
					mob.setItemSlot(slot, ItemStack.EMPTY);
			if (monster instanceof LivingEntity living) {
				living.yBodyRot = living.yBodyRotO = living.yHeadRot = living.yHeadRotO = FACING;
				living.setYRot(FACING);
				living.yRotO = FACING;
			}
			MONSTERS.put(data, monster);
		}
		return monster;
	}
}
