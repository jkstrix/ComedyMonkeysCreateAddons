package com.comedymonkey.createaddons.client.wastes.bot;

import org.joml.Matrix4f;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.tradebot.GlowModel;
import com.comedymonkey.createaddons.content.wastes.ConduitPylonBlock;
import com.comedymonkey.createaddons.content.wastes.ConduitPylonBlockEntity;
import com.comedymonkey.createaddons.content.wastes.bot.CityBot;
import com.comedymonkey.createaddons.content.wastes.bot.Custodian;
import com.comedymonkey.createaddons.content.wastes.bot.Sentry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a Clockwork Wastes bot at full size (1 px = 1/16 block), its lenses, seams, studs and sigil at full brightness
 * from a second texture holding only those pixels (as the Juggernaut's runes). The Sentry is turned onto the face it's
 * mounted on; a Scuttler clinging to a wall faces up it; the Custodian adds its tether cables (CustodianTethers), its
 * shield's shell (rippling where it's struck) and the arcs from its standing pylons to the sockets on its back while it's
 * shielded (P32, brief). None of them topples when it dies: each plays its own death clip.
 */
public class BotRenderer<T extends CityBot> extends MobRenderer<T, BotModel<T>> {
	/** Starlight, muted (the world brief's star and core tones). */
	static final int STAR = 0x7f76a8, CORE = 0xb8b0d8;

	private final ResourceLocation texture;

	public BotRenderer(EntityRendererProvider.Context context, BotModel.Rig rig, float shadow) {
		super(context, new BotModel<>(rig, context.bakeLayer(rig.layer())), shadow);
		this.texture = ComedyMonkeysCreateAddons.asResource("textures/entity/" + rig.id() + ".png");
		ResourceLocation glow = ComedyMonkeysCreateAddons.asResource("textures/entity/" + rig.id() + "_glow.png");
		addLayer(new Glow<>(this, GlowModel.of(rig.geometry(), glow, getModel().parts(), rig.id()), glow));
	}

	/** Each bot plays its own death (a Scuttler's legs curl in, a Sentry droops, the Custodian kneels to the vault). */
	@Override
	protected float getFlipDegrees(T bot) {
		return 0;
	}

	public static BotRenderer<com.comedymonkey.createaddons.content.wastes.bot.Scuttler> scuttler(EntityRendererProvider.Context context) {
		return new BotRenderer<>(context, BotModel.SCUTTLER, 0.4f) {
			/** Clinging to a wall, it faces up it, square to it (its climb clip has the wall 6 px in front of it). */
			@Override
			protected void setupRotations(com.comedymonkey.createaddons.content.wastes.bot.Scuttler s, PoseStack ms, float bob, float bodyYaw,
				float partialTick, float scale) {
				Direction wall = s.climbingWall();
				super.setupRotations(s, ms, bob, wall != null ? wall.toYRot() : bodyYaw, partialTick, scale);
			}
		};
	}

	public static BotRenderer<Sentry> sentry(EntityRendererProvider.Context context) {
		return new BotRenderer<>(context, BotModel.SENTRY, 0.5f) {
			/** Fixed to its face: turned so its mount lies flat on the wall or floor (Sentry.frame, the same turn). */
			@Override
			protected void setupRotations(Sentry sentry, PoseStack ms, float bob, float bodyYaw, float partialTick, float scale) {
				Direction face = sentry.face();
				if (face != Direction.UP) {
					ms.translate(0, 0.5, 0);
					ms.mulPose(face.getRotation());
					ms.translate(0, -0.5, 0);
				}
				ms.mulPose(Axis.YP.rotationDegrees(180));
			}
		};
	}

	public static BotRenderer<Custodian> custodian(EntityRendererProvider.Context context) {
		return new CustodianRenderer(context);
	}

	/**
	 * The Custodian: its model, then its tether cables from where the posed model has its sockets this frame (found by a
	 * layer while the model's pose is on the stack), its shockwave ring, and while it's shielded its shell and arcs.
	 */
	static final class CustodianRenderer extends BotRenderer<Custodian> {
		/** This frame's sockets, relative to its position (world axes, blocks); filled by the probe layer. */
		private final CustodianTethers.Socket[] sockets = new CustodianTethers.Socket[4];
		private boolean probed;
		private final Matrix4f base = new Matrix4f();

		CustodianRenderer(EntityRendererProvider.Context context) {
			super(context, BotModel.CUSTODIAN, 0.9f);
			addLayer(new SocketProbe(this));
		}

		@Override
		public void render(Custodian custodian, float yaw, float partialTick, PoseStack ms, MultiBufferSource buffers, int light) {
			base.set(ms.last().pose()).invert();
			probed = false;
			super.render(custodian, yaw, partialTick, ms, buffers, light);
			CustodianTethers.draw(custodian, partialTick, ms, buffers, probed ? sockets : null);
			double ring = custodian.ringAge(partialTick);
			if (ring >= 0)
				ring(custodian, ring, partialTick, ms, buffers.getBuffer(RenderType.lightning()));
			if (custodian.dormant() || !custodian.shielded())
				return;
			VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
			shell(custodian, partialTick, ms, vc);
			arcs(custodian, partialTick, ms, vc, probed ? sockets : null);
		}

		/** Its tether cables reach its pylons: drawn while any of them is in view. */
		@Override
		public boolean shouldRender(Custodian custodian, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
			if (super.shouldRender(custodian, frustum, x, y, z))
				return true;
			for (BlockPos p : custodian.pylonPositions())
				if (frustum.isVisible(new net.minecraft.world.phys.AABB(p).expandTowards(0, 2, 0)))
					return true;
			return false;
		}

		/** Where the posed model has each socket, while its pose is on the stack (the cable leaves 1.5 px behind the plug). */
		void probe(PoseStack ms) {
			BotModel<Custodian> model = getModel();
			for (int k = 0; k < 4; k++) {
				java.util.List<net.minecraft.client.model.geom.ModelPart> chain = model.socketChain(k);
				if (chain == null) {
					sockets[k] = null;
					continue;
				}
				ms.pushPose();
				for (net.minecraft.client.model.geom.ModelPart part : chain)
					part.translateAndRotate(ms);
				org.joml.Vector3f a = ms.last().pose().transformPosition(new org.joml.Vector3f(0, 0, 1 / 16f));
				org.joml.Vector3f b = ms.last().pose().transformPosition(new org.joml.Vector3f(0, 0, 3 / 16f));
				ms.popPose();
				base.transformPosition(a);
				base.transformPosition(b);
				Vec3 back = new Vec3(b.x - a.x, b.y - a.y, b.z - a.z).normalize();
				sockets[k] = new CustodianTethers.Socket(new Vec3(a.x, a.y, a.z).add(back.scale(1.5 / 16)), back);
			}
			probed = true;
		}
	}

	/** A layer that draws nothing: it reads where the sockets are while the model's pose is on the stack. */
	private static final class SocketProbe extends RenderLayer<Custodian, BotModel<Custodian>> {
		private final CustodianRenderer renderer;

		SocketProbe(CustodianRenderer renderer) {
			super(renderer);
			this.renderer = renderer;
		}

		@Override
		public void render(PoseStack ms, MultiBufferSource buffers, int light, Custodian c, float limbSwing, float limbSwingAmount, float partialTick,
			float ageInTicks, float netHeadYaw, float headPitch) {
			renderer.probe(ms);
		}
	}

	@Override
	public ResourceLocation getTextureLocation(T bot) {
		return texture;
	}

	// ---------------------------------------------------------------- the Custodian's shield and arcs

	/** A translucent starlight shell round it; a hit sets it rippling for half a second (brief: "like a soap film"). */
	static void shell(Custodian c, float partialTick, PoseStack ms, VertexConsumer vc) {
		float since = c.tickCount + partialTick - c.shieldHitAt();
		float ripple = since >= 0 && since < 10 ? 1 - since / 10 : 0;
		float alpha = 0.10f + 0.25f * ripple;
		float wobble = 0.06f * ripple;
		Matrix4f m = ms.last().pose();
		float t = c.tickCount + partialTick;
		int rings = 10, segs = 16;
		for (int i = 0; i < rings; i++)
			for (int j = 0; j < segs; j++) {
				Vec3 a = shellPoint(i, j, rings, segs, t, wobble), b = shellPoint(i + 1, j, rings, segs, t, wobble);
				Vec3 d = shellPoint(i + 1, j + 1, rings, segs, t, wobble), e = shellPoint(i, j + 1, rings, segs, t, wobble);
				quad(vc, m, a, b, d, e, STAR, alpha);
			}
	}

	private static Vec3 shellPoint(int i, int j, int rings, int segs, float t, float wobble) {
		double th = Math.PI * i / rings, ph = Mth.TWO_PI * j / segs;
		double r = 1 + wobble * Math.sin(th * 6 + t * 1.3) * Math.cos(ph * 3);
		return new Vec3(Math.sin(th) * Math.cos(ph) * 1.0 * r, 1.55 + Math.cos(th) * 1.75 * r, Math.sin(th) * Math.sin(ph) * 1.0 * r);
	}

	/** Segments round the shockwave ring. */
	private static final int RING_SEGS = 72;
	/** The ring's band: the bright crest, and the starlight trailing behind it, in blocks. */
	private static final float CREST = 0.14f, TRAIL = 0.7f, CREST_HEIGHT = 0.22f;

	/**
	 * The shockwave (D56): a starlight ring running out along the floor from where it stamped. Its crest is the core
	 * tone, a low lip of light; behind it the band fades out in the edge tone. It dims over its last two blocks.
	 */
	static void ring(Custodian c, double age, float partialTick, PoseStack ms, VertexConsumer vc) {
		double r = c.ringRadius(age), end = c.ringEnd();
		float fade = (float) Mth.clamp((end - r) / 2, 0, 1);
		Vec3 off = c.ringCentre().subtract(c.getPosition(partialTick));
		Matrix4f m = ms.last().pose();
		float y = (float) off.y + 0.03f;
		for (int i = 0; i < RING_SEGS; i++) {
			double a0 = Mth.TWO_PI * i / RING_SEGS, a1 = Mth.TWO_PI * (i + 1) / RING_SEGS;
			float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
			// the trailing band on the floor: edge tone, brightest at the crest, gone at its back
			float rIn = (float) Math.max(0, r - CREST - TRAIL), rMid = (float) Math.max(0, r - CREST), rOut = (float) r;
			band(vc, m, off, y, c0, s0, c1, s1, rIn, rMid, STAR, 0, 0.45f * fade);
			// the crest: the core tone, and only there
			band(vc, m, off, y, c0, s0, c1, s1, rMid, rOut, CORE, 0.85f * fade, 0.85f * fade);
			// its low lip of light, so it reads from the side
			wall(vc, m, off, y, c0, s0, c1, s1, rOut, CREST_HEIGHT, CORE, 0.6f * fade);
		}
	}

	/** A flat strip of the ring between two radii, its alpha running from the inner edge to the outer. */
	private static void band(VertexConsumer vc, Matrix4f m, Vec3 off, float y, float c0, float s0, float c1, float s1, float rIn, float rOut,
		int rgb, float alphaIn, float alphaOut) {
		if (rOut <= rIn)
			return;
		float x = (float) off.x, z = (float) off.z;
		float[][] p = { { x + c0 * rIn, z + s0 * rIn, alphaIn }, { x + c0 * rOut, z + s0 * rOut, alphaOut },
			{ x + c1 * rOut, z + s1 * rOut, alphaOut }, { x + c1 * rIn, z + s1 * rIn, alphaIn } };
		vertices(vc, m, rgb, y, p, false);
		vertices(vc, m, rgb, y, p, true);
	}

	/** The crest's upright lip: full at the floor, gone at its top. */
	private static void wall(VertexConsumer vc, Matrix4f m, Vec3 off, float y, float c0, float s0, float c1, float s1, float r, float h, int rgb, float alpha) {
		float x = (float) off.x, z = (float) off.z;
		int red = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
		float[][] p = { { x + c0 * r, y, z + s0 * r, alpha }, { x + c0 * r, y + h, z + s0 * r, 0 }, { x + c1 * r, y + h, z + s1 * r, 0 },
			{ x + c1 * r, y, z + s1 * r, alpha } };
		for (int k = 0; k < 4; k++)
			vc.addVertex(m, p[k][0], p[k][1], p[k][2]).setColor(red, g, b, (int) (p[k][3] * 255));
		for (int k = 3; k >= 0; k--)
			vc.addVertex(m, p[k][0], p[k][1], p[k][2]).setColor(red, g, b, (int) (p[k][3] * 255));
	}

	private static void vertices(VertexConsumer vc, Matrix4f m, int rgb, float y, float[][] p, boolean back) {
		int red = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
		for (int k = 0; k < 4; k++) {
			float[] v = p[back ? 3 - k : k];
			vc.addVertex(m, v[0], y, v[1]).setColor(red, g, b, (int) (v[2] * 255));
		}
	}

	/** An arc from each standing pylon's crown to its cable's socket on its back, jagged afresh every other tick. */
	static void arcs(Custodian c, float partialTick, PoseStack ms, VertexConsumer vc, CustodianTethers.Socket[] sockets) {
		Vec3 at = c.getPosition(partialTick);
		Matrix4f m = ms.last().pose();
		java.util.List<BlockPos> pylons = c.pylonPositions();
		int[] socketOf = CustodianTethers.sockets(c, pylons);
		for (int i = 0; i < pylons.size(); i++) {
			BlockPos pos = pylons.get(i);
			int socket = socketOf[i];
			ConduitPylonBlockEntity be = ConduitPylonBlock.pylonAt(c.level(), pos);
			if (be == null || !be.isStanding())
				continue;
			Vec3 from = be.arcPoint().subtract(at);
			Vec3 to = sockets != null && sockets[socket] != null ? sockets[socket].p() : socket(c, socket, partialTick).subtract(at);
			RandomSource random = RandomSource.create(pos.asLong() * 31 + c.tickCount / 2);
			int n = Math.max(4, (int) (from.distanceTo(to) * 1.5));
			Vec3 prev = from;
			for (int s = 1; s <= n; s++) {
				Vec3 p = from.lerp(to, s / (double) n);
				if (s < n)
					p = p.add((random.nextFloat() - 0.5) * 0.35, (random.nextFloat() - 0.5) * 0.35, (random.nextFloat() - 0.5) * 0.35);
				ribbon(vc, m, prev, p, 0.05f, CORE, 0.55f);
				ribbon(vc, m, prev, p, 0.12f, STAR, 0.18f);
				prev = p;
			}
		}
	}

	private static Vec3 socket(Custodian c, int k, float partialTick) {
		Vec3 now = c.socket(k);
		return now.subtract(c.position()).add(c.getPosition(partialTick));
	}

	/** A line drawn as two crossed flat strips. */
	static void ribbon(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, float width, int rgb, float alpha) {
		Vec3 d = b.subtract(a);
		if (d.lengthSqr() < 1e-6)
			return;
		Vec3 side = d.cross(new Vec3(0, 1, 0));
		if (side.lengthSqr() < 1e-6)
			side = d.cross(new Vec3(1, 0, 0));
		side = side.normalize().scale(width / 2);
		Vec3 up = side.cross(d).normalize().scale(width / 2);
		quad(vc, m, a.add(side), b.add(side), b.subtract(side), a.subtract(side), rgb, alpha);
		quad(vc, m, a.add(up), b.add(up), b.subtract(up), a.subtract(up), rgb, alpha);
	}

	static void quad(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int rgb, float alpha) {
		int r = rgb >> 16 & 255, g = rgb >> 8 & 255, bl = rgb & 255, al = (int) (alpha * 255);
		for (Vec3 p : new Vec3[] { a, b, c, d })
			vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, bl, al);
		// and the back face, so it shows from inside and out
		for (Vec3 p : new Vec3[] { d, c, b, a })
			vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, bl, al);
	}

	private static class Glow<T extends CityBot> extends RenderLayer<T, BotModel<T>> {
		private final GlowModel glow;
		private final ResourceLocation texture;

		Glow(RenderLayerParent<T, BotModel<T>> parent, GlowModel glow, ResourceLocation texture) {
			super(parent);
			this.glow = glow;
			this.texture = texture;
		}

		@Override
		public void render(PoseStack ms, MultiBufferSource buffers, int light, T bot, float limbSwing, float limbSwingAmount, float partialTick,
			float ageInTicks, float netHeadYaw, float headPitch) {
			if (bot.isInvisible())
				return;
			glow.render(ms, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), OverlayTexture.NO_OVERLAY);
		}
	}
}
