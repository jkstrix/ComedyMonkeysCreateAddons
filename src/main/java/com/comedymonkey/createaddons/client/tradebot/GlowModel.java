package com.comedymonkey.createaddons.client.tradebot;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.slf4j.Logger;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.server.packs.resources.Resource;

/**
 * The cubes of one of Trade Bot's models that show anything on his glow texture, posed just as the whole model is, so
 * the glow pass draws only those rather than the whole model a second time. Everywhere else the glow texture is
 * clear, and the cutout pass drops clear pixels, so it looks the same. If the texture can't be read, every cube is kept.
 */
final class GlowModel {
	private static final Logger LOGGER = LogUtils.getLogger();
	private final ModelPart root;
	/** Each bone of the whole model and its twin here, in the same order. */
	private final ModelPart[] from, to;

	private GlowModel(ModelPart root, ModelPart[] from, ModelPart[] to) {
		this.root = root;
		this.from = from;
		this.to = to;
	}

	/** The glowing cubes of the model built from these root bones, following {@code parts}, that model's bones by name. */
	static GlowModel of(Map<String, ModelPart> parts, String... roots) {
		TradeBotGeometry geometry = TradeBotGeometry.get();
		ModelPart root = geometry.layer(glowing(geometry), roots).bakeRoot();
		Map<String, ModelPart> glowParts = geometry.parts(root, roots);
		ModelPart[] from = new ModelPart[parts.size()], to = new ModelPart[parts.size()];
		int i = 0;
		for (Map.Entry<String, ModelPart> entry : parts.entrySet()) {
			from[i] = entry.getValue();
			to[i++] = glowParts.get(entry.getKey());
		}
		return new GlowModel(root, from, to);
	}

	/** Takes the whole model's current pose, then draws the glowing cubes at full brightness. */
	void render(PoseStack ms, VertexConsumer vc, int overlay) {
		for (int i = 0; i < from.length; i++) {
			ModelPart a = from[i], b = to[i];
			b.x = a.x;
			b.y = a.y;
			b.z = a.z;
			b.xRot = a.xRot;
			b.yRot = a.yRot;
			b.zRot = a.zRot;
			b.xScale = a.xScale;
			b.yScale = a.yScale;
			b.zScale = a.zScale;
			b.visible = a.visible;
			b.skipDraw = a.skipDraw;
		}
		root.render(ms, vc, LightTexture.FULL_BRIGHT, overlay);
	}

	/**
	 * Which cubes have any pixel that isn't clear on the glow texture, anywhere in their box-UV layout (the whole
	 * rectangle its six faces are laid out in, so never fewer than the faces really cover).
	 */
	static Predicate<TradeBotGeometry.Cube> glowing(TradeBotGeometry geometry) {
		Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(TradeBotRenderer.GLOW);
		if (resource.isEmpty())
			return cube -> true;
		try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
			float sx = image.getWidth() / (float) geometry.textureWidth(), sy = image.getHeight() / (float) geometry.textureHeight();
			// Read every pixel's alpha now: the image is closed when this returns
			boolean[][] lit = new boolean[image.getWidth()][image.getHeight()];
			for (int x = 0; x < image.getWidth(); x++)
				for (int y = 0; y < image.getHeight(); y++)
					lit[x][y] = (image.getPixelRGBA(x, y) >>> 24) != 0;
			return cube -> {
				float dx = cube.to()[0] - cube.from()[0], dy = cube.to()[1] - cube.from()[1], dz = cube.to()[2] - cube.from()[2];
				int x0 = Math.max(0, (int) Math.floor(cube.u() * sx)), y0 = Math.max(0, (int) Math.floor(cube.v() * sy));
				int x1 = Math.min(lit.length, (int) Math.ceil((cube.u() + 2 * (dx + dz)) * sx));
				int y1 = Math.min(lit[0].length, (int) Math.ceil((cube.v() + dy + dz) * sy));
				for (int x = x0; x < x1; x++)
					for (int y = y0; y < y1; y++)
						if (lit[x][y])
							return true;
				return false;
			};
		} catch (IOException | RuntimeException e) {
			LOGGER.warn("Couldn't read Trade Bot's glow texture; drawing his whole model for the glow", e);
			return cube -> true;
		}
	}
}
