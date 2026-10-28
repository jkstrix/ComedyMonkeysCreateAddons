package com.comedymonkey.createaddons.client;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.light.FestoonBlockEntity;
import com.comedymonkey.createaddons.content.light.FestoonHookBlock;
import com.comedymonkey.createaddons.content.light.Festoons;
import com.comedymonkey.createaddons.content.light.LightBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Festoon Lights (Festoons): each string a hook holds, drawn from its eye to the far hook's, sagging, with a bulb on a
 * socket every 2 blocks: the unlit glass, or lit in its colour (as strong as a Light Strip's line, D13), flickering when
 * the run starves.
 */
public class FestoonRenderer extends SafeBlockEntityRenderer<FestoonBlockEntity> {
	/** The flex's half thickness and the bulbs' size (blocks). */
	private static final float FLEX = 0.3f / 16, SOCKET = 0.5f / 16, BULB = 1.25f / 16;

	public FestoonRenderer(BlockEntityRendererProvider.Context context) {}

	private static TextureAtlasSprite sprite(String name) {
		return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ComedyMonkeysCreateAddons.asResource("block/light/" + name));
	}

	@Override
	protected void renderSafe(FestoonBlockEntity be, float pt, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		if (be.getLevel() == null)
			return;
		BlockPos pos = be.getBlockPos();
		Direction facing = be.getBlockState().getValue(FestoonHookBlock.FACING);
		long time = be.getLevel().getGameTime();
		boolean lit = be.phase() != LightBlockEntity.Phase.DARK && LightRenderer.lit(be, time, pt);
		int glow = LightRenderer.runGlow(be.dye(), be.step());
		VertexConsumer vc = buffer.getBuffer(RenderType.cutout());
		TextureAtlasSprite flex = sprite("flex"), steel = sprite("steel"), bulb = sprite(lit ? "glow_white" : "glass_off");
		Vec3 origin = Vec3.atLowerCornerOf(pos);
		for (FestoonBlockEntity.Span span : be.spans()) {
			if (!span.held())
				continue;
			Vec3 a = Festoons.eye(pos, facing), b = Festoons.eye(span.other(), span.facing());
			double length = a.distanceTo(b);
			int segments = Math.max(8, (int) Math.ceil(length * 3));
			for (int i = 0; i < segments; i++) {
				Vec3 p0 = Festoons.along(a, b, (double) i / segments), p1 = Festoons.along(a, b, (double) (i + 1) / segments);
				int l = LevelRenderer.getLightColor(be.getLevel(), BlockPos.containing(p0.add(p1).scale(0.5)));
				beam(vc, ms.last(), p0.subtract(origin), p1.subtract(origin), FLEX, flex, 0xFFFFFF, l);
			}
			for (double t : Festoons.bulbs(length)) {
				Vec3 at = Festoons.along(a, b, t);
				int l = LevelRenderer.getLightColor(be.getLevel(), BlockPos.containing(at));
				Vec3 top = at.subtract(origin), neck = top.subtract(0, 1.25 / 16, 0), base = neck.subtract(0, 2.5 / 16, 0);
				beam(vc, ms.last(), top, neck, SOCKET, steel, 0xFFFFFF, l);
				beam(vc, ms.last(), neck, base, BULB, bulb, lit ? glow : 0xFFFFFF, lit ? LightTexture.FULL_BRIGHT : l);
			}
		}
	}

	/** A square rod from a to b, half as thick as {@code half}, its four sides and two ends. */
	private static void beam(VertexConsumer vc, PoseStack.Pose pose, Vec3 a, Vec3 b, float half, TextureAtlasSprite sprite, int rgb, int light) {
		Vec3 d = b.subtract(a).normalize();
		Vec3 u = d.cross(Math.abs(d.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).normalize().scale(half);
		Vec3 v = d.cross(u).normalize().scale(half);
		Vec3[] ring = {u.add(v), u.subtract(v), u.scale(-1).subtract(v), u.scale(-1).add(v)};
		float r = (rgb >> 16 & 255) / 255f, g = (rgb >> 8 & 255) / 255f, bl = (rgb & 255) / 255f;
		float u0 = sprite.getU(0.4f), u1 = sprite.getU(0.6f), v0 = sprite.getV(0.2f), v1 = sprite.getV(0.8f);
		for (int i = 0; i < 4; i++) {
			Vec3 s0 = ring[i], s1 = ring[(i + 1) % 4], n = s0.add(s1).normalize();
			quad(vc, pose, a.add(s0), b.add(s0), b.add(s1), a.add(s1), n, r, g, bl, u0, u1, v0, v1, light);
		}
		quad(vc, pose, a.add(ring[3]), a.add(ring[2]), a.add(ring[1]), a.add(ring[0]), d.scale(-1), r, g, bl, u0, u1, v0, v1, light);
		quad(vc, pose, b.add(ring[0]), b.add(ring[1]), b.add(ring[2]), b.add(ring[3]), d, r, g, bl, u0, u1, v0, v1, light);
	}

	private static void quad(VertexConsumer vc, PoseStack.Pose pose, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 n, float r, float g, float b,
							 float u0, float u1, float v0, float v1, int light) {
		vertex(vc, pose, p0, n, r, g, b, u0, v0, light);
		vertex(vc, pose, p1, n, r, g, b, u1, v0, light);
		vertex(vc, pose, p2, n, r, g, b, u1, v1, light);
		vertex(vc, pose, p3, n, r, g, b, u0, v1, light);
	}

	private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vec3 p, Vec3 n, float r, float g, float b, float u, float v, int light) {
		vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, 1f).setUv(u, v).setLight(light)
			.setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
	}

	/** Its strings reach their far hooks. */
	@Override
	public AABB getRenderBoundingBox(FestoonBlockEntity be) {
		AABB box = new AABB(be.getBlockPos()).inflate(1);
		for (FestoonBlockEntity.Span span : be.spans())
			if (span.held())
				box = box.minmax(new AABB(span.other()).inflate(1));
		return box;
	}

	@Override
	public int getViewDistance() {
		return 128;
	}
}
