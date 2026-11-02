package com.comedymonkey.createaddons.client;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.light.FestoonBlockEntity;
import com.comedymonkey.createaddons.content.light.FestoonHookBlock;
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

/**
 * Festoon Lights (Festoons): each string a hook holds, drawn from its eye to the far hook's, sagging, with a bulb on a
 * socket every 2 blocks: the unlit glass, or lit in its colour (as strong as a Light Strip's line, D13), flickering when
 * the run starves. With Flywheel on, {@link FestoonVisual} draws them instead; this stays for Ponder and whenever
 * Flywheel can't draw. Both lay the strings out with {@link FestoonGeometry}: here once per change of strings, then
 * only the light is looked up a frame.
 */
public class FestoonRenderer extends SafeBlockEntityRenderer<FestoonBlockEntity> {
	/** How far away its strings are drawn (blocks). */
	static final int VIEW_DISTANCE = 128;
	/** The textures, looked up once per resource reload: flex, steel, unlit glass, lit glass. */
	private static volatile TextureAtlasSprite[] sprites;

	/** Each hook's strings as last laid out (render thread only; gone with the hook). */
	private final Map<FestoonBlockEntity, Laid> laid = new WeakHashMap<>();
	private final BlockPos.MutableBlockPos cellPos = new BlockPos.MutableBlockPos();
	/** A corner being drawn: x, y, z and normal. */
	private final float[] corner = new float[6];

	/** What a hook's rods were laid out from, the rods, and room for the light where they hang. */
	private record Laid(BlockPos pos, Direction facing, List<FestoonBlockEntity.Span> spans, FestoonGeometry.Layout layout, int[] lights) {}

	public FestoonRenderer(BlockEntityRendererProvider.Context context) {}

	/** Flex, steel, unlit glass and lit glass, from the block atlas. */
	static TextureAtlasSprite[] sprites() {
		TextureAtlasSprite[] s = sprites;
		if (s == null)
			sprites = s = new TextureAtlasSprite[] {sprite("flex"), sprite("steel"), sprite("glass_off"), sprite("glow_white")};
		return s;
	}

	private static TextureAtlasSprite sprite(String name) {
		return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ComedyMonkeysCreateAddons.asResource("block/light/" + name));
	}

	/** After the models (and the atlas) are baked again. */
	public static void forget() {
		sprites = null;
	}

	@Override
	protected void renderSafe(FestoonBlockEntity be, float pt, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		if (be.getLevel() == null)
			return;
		Laid strings = laidOut(be);
		FestoonGeometry.Layout layout = strings.layout();
		if (layout.rods() == 0)
			return;
		long time = be.getLevel().getGameTime();
		boolean lit = be.phase() != LightBlockEntity.Phase.DARK && LightRenderer.lit(be, time, pt);
		int glow = lit ? LightRenderer.runGlow(be.dye(), be.step()) : 0xFFFFFF;
		VertexConsumer vc = buffer.getBuffer(RenderType.cutout());
		TextureAtlasSprite[] s = sprites();
		TextureAtlasSprite bulb = lit ? s[3] : s[2];
		// (the light where each block of string passes, looked up once a frame)
		long[] cells = layout.cells();
		int[] lights = strings.lights();
		for (int i = 0; i < cells.length; i++)
			lights[i] = LevelRenderer.getLightColor(be.getLevel(), cellPos.set(cells[i]));
		PoseStack.Pose pose = ms.last();
		for (int i = 0; i < layout.rods(); i++) {
			int l = lights[layout.cell()[i]];
			switch (layout.parts()[i]) {
				case FLEX -> rod(vc, pose, layout.frame(), i, s[0], 0xFFFFFF, l, corner);
				case SOCKET -> rod(vc, pose, layout.frame(), i, s[1], 0xFFFFFF, l, corner);
				case BULB -> rod(vc, pose, layout.frame(), i, bulb, glow, lit ? LightTexture.FULL_BRIGHT : l, corner);
			}
		}
	}

	/** Its rods, laid out again only when its strings (or where it hangs) changed. */
	private Laid laidOut(FestoonBlockEntity be) {
		BlockPos pos = be.getBlockPos();
		Direction facing = be.getBlockState().getValue(FestoonHookBlock.FACING);
		Laid strings = laid.get(be);
		if (strings == null || strings.facing() != facing || !strings.pos().equals(pos) || !strings.spans().equals(be.spans())) {
			List<FestoonBlockEntity.Span> spans = List.copyOf(be.spans());
			FestoonGeometry.Layout layout = FestoonGeometry.layOut(pos, facing, spans);
			strings = new Laid(pos.immutable(), facing, spans, layout, new int[layout.cells().length]);
			laid.put(be, strings);
		}
		return strings;
	}

	/** Rod {@code i}: the rod shape stretched into place by its frame, every face showing the middle of the texture. */
	private static void rod(VertexConsumer vc, PoseStack.Pose pose, double[] frame, int i, TextureAtlasSprite sprite, int rgb, int light,
							float[] c) {
		float r = (rgb >> 16 & 255) / 255f, g = (rgb >> 8 & 255) / 255f, b = (rgb & 255) / 255f;
		float[] rod = FestoonGeometry.ROD;
		for (int k = 0; k < FestoonGeometry.ROD_VERTICES; k++) {
			int p = k * FestoonGeometry.ROD_STRIDE;
			FestoonGeometry.corner(frame, i, k, c);
			vc.addVertex(pose, c[0], c[1], c[2]).setColor(r, g, b, 1f).setUv(sprite.getU(rod[p + 6]), sprite.getV(rod[p + 7]))
				.setLight(light).setNormal(pose, c[3], c[4], c[5]);
		}
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
		return VIEW_DISTANCE;
	}
}
