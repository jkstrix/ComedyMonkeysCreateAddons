package com.comedymonkey.createaddons.client;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/**
 * Where the camera is from a block that's being drawn, in the block's own coordinates (0 to 1 across it). Worked out
 * from the pose, whose origin is the camera, so it's right on contraptions and ships too. Not known in Ponder, whose
 * pose starts somewhere else, nor in a shader pack's shadow pass, which draws from the sun: then everything is drawn,
 * but for the creatures and items in the windows, which a shadow pass leaves out.
 */
final class CameraView {
	/** The glass of every glass-fronted machine is set this far in from its front face (1.2 px), less a little to spare. */
	private static final double GLASS_INSET = 1.2 / 16 - 0.25 / 16;
	/** Only ever used on the render thread. */
	private static final Matrix4f INVERSE = new Matrix4f();
	/**
	 * Iris's (or Oculus's) "is it drawing the shadow pass" check, bound to its API and typed exactly, so asking it for
	 * every block is as cheap as a plain call. The pass changes partway through a frame, so it's asked each time.
	 */
	@Nullable
	private static final MethodHandle SHADOW_PASS = shadowPassCheck();
	private static final CameraView PONDER = new CameraView(0.5, 0.5, 0.5, false, false, null, false);
	private static final CameraView SHADOW = new CameraView(0.5, 0.5, 0.5, false, true, null, false);
	/**
	 * Display creatures drawn per frame, all windows together, before they're shared out: a big base of cages and tanks
	 * can't each draw theirs. Ponder isn't counted, nor anything drawn outside a frame (the website's renders).
	 */
	static final int CREATURES_PER_FRAME = 64;
	/** The creature tiers' outer edges (all, two, one, then none), and how far inside one the camera must come back. */
	private static final double[] TIER_OUT = {10, 16, 32};
	/** The item tiers' outer edges (all, one, then none). */
	private static final double[] ITEM_TIER_OUT = {16, 32};
	private static final double TIER_MARGIN = 1;
	/** Between a frame's start and end: the game is drawing the world, rather than something drawing it for a picture. */
	private static boolean inFrame;
	/** Creatures the windows asked for, and windows asking for any: this frame so far, and all of last frame. */
	private static int wanted, wanting, wantedLast, wantingLast;
	/** Each window's creature and item tiers as last drawn, for the hysteresis. */
	private static final Map<BlockEntity, Integer> TIERS = new WeakHashMap<>(), ITEM_TIERS = new WeakHashMap<>();

	private final double x, y, z;
	private final boolean known, shadow;
	/** The window drawn, for its tier last time; and whether it's drawn inside a frame (else, as in Ponder, all of it). */
	@Nullable
	private final BlockEntity owner;
	private final boolean framed;

	private CameraView(double x, double y, double z, boolean known, boolean shadow, @Nullable BlockEntity owner, boolean framed) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.known = known;
		this.shadow = shadow;
		this.owner = owner;
		this.framed = framed;
	}

	/** Call with the pose as the renderer was given it, before it's moved. */
	static CameraView of(BlockEntity be, PoseStack ms) {
		if (be.getLevel() instanceof PonderLevel)
			return PONDER;
		if (inShadowPass())
			return SHADOW;
		ms.last().pose().invertAffine(INVERSE);
		return new CameraView(INVERSE.m30(), INVERSE.m31(), INVERSE.m32(), true, false, be, inFrame);
	}

	static void onFrame(RenderFrameEvent.Pre event) {
		startFrame();
	}

	static void onFrameEnd(RenderFrameEvent.Post event) {
		endFrame();
	}

	/** A frame starts: what this one asks for is counted afresh, and last frame's totals share out its budget. */
	static void startFrame() {
		wantedLast = wanted;
		wantingLast = wanting;
		wanted = wanting = 0;
		inFrame = true;
	}

	static void endFrame() {
		inFrame = false;
	}

	/**
	 * Whether the camera is behind the glass in the block's {@code front} face. For a box that's opaque everywhere but
	 * that window, nothing inside can then be seen: every line to the inside crosses another face. Never from inside
	 * the block itself.
	 */
	boolean behindGlass(Direction front) {
		if (!known || x >= 0 && x <= 1 && y >= 0 && y <= 1 && z >= 0 && z <= 1)
			return false;
		double out = (x - 0.5) * front.getStepX() + (y - 0.5) * front.getStepY() + (z - 0.5) * front.getStepZ();
		return out < 0.5 - GLASS_INSET;
	}

	/** Whether this is a shader's shadow pass, where nothing small needs drawing. */
	boolean shadow() {
		return shadow;
	}

	/** Whether the camera's place is known; when it isn't, draw everything. */
	boolean known() {
		return known;
	}

	/** Squared distance from the camera to the block's centre, in blocks; 0 when it isn't known. */
	double distanceSqr() {
		if (!known)
			return 0;
		double dx = x - 0.5, dy = y - 0.5, dz = z - 0.5;
		return dx * dx + dy * dy + dz * dz;
	}

	/**
	 * How many of a window's {@code all} display creatures to draw: all within 10 blocks, two within 16, one within 32,
	 * none further or in a shadow pass (coming back, a block inside each edge); all in Ponder or outside a frame. When
	 * last frame asked for more than {@link #CREATURES_PER_FRAME}, every window gets an even share of it (a near one at
	 * least one), so they all thin out together, whatever order they're drawn in.
	 */
	int creatures(int all) {
		if (shadow)
			return 0;
		if (!known || !framed)
			return all;
		int tier = tier(owner, distanceSqr());
		int n = Math.min(all, tier == 0 ? all : tier == 1 ? 2 : tier == 2 ? 1 : 0);
		if (n > 0) {
			wanted += n;
			wanting++;
		}
		if (wantedLast > CREATURES_PER_FRAME) {
			int share = CREATURES_PER_FRAME / Math.max(1, wantingLast);
			n = Math.min(n, Math.max(share, tier == 0 ? 1 : 0));
		}
		return n;
	}

	/** The creature tier (0 all, 1 two, 2 one, 3 none) at this distance, going on from {@code owner}'s last one. */
	static int tier(@Nullable BlockEntity owner, double distanceSqr) {
		return tier(TIER_OUT, TIERS, owner, distanceSqr);
	}

	/** The item tier (0 all, 1 one, 2 none) at this distance, going on from {@code owner}'s last one. */
	static int itemTier(@Nullable BlockEntity owner, double distanceSqr) {
		return tier(ITEM_TIER_OUT, ITEM_TIERS, owner, distanceSqr);
	}

	/** The tier between these {@code edges} at this distance; coming closer, a nearer one only a block inside its edge. */
	private static int tier(double[] edges, Map<BlockEntity, Integer> lastTiers, @Nullable BlockEntity owner, double distanceSqr) {
		int now = 0;
		while (now < edges.length && distanceSqr > edges[now] * edges[now])
			now++;
		if (owner == null)
			return now;
		Integer last = lastTiers.get(owner);
		while (last != null && now < last && distanceSqr > (edges[now] - TIER_MARGIN) * (edges[now] - TIER_MARGIN))
			now++;
		if (last == null || last != now)
			lastTiers.put(owner, now);
		return now;
	}

	/**
	 * How many of the {@code all} items a window shows (food, chow, the tray) to draw: one past 16 blocks, none past 32
	 * or in a shadow pass (coming back, a block inside each edge); all in Ponder or outside a frame.
	 */
	int items(int all) {
		if (shadow)
			return 0;
		if (!known || !framed)
			return all;
		int tier = itemTier(owner, distanceSqr());
		return tier == 0 ? all : tier == 1 ? Math.min(all, 1) : 0;
	}

	private static boolean inShadowPass() {
		if (SHADOW_PASS == null)
			return false;
		try {
			return (boolean) SHADOW_PASS.invokeExact();
		} catch (Throwable e) {
			return false;
		}
	}

	/** {@code IrisApi.getInstance().isRenderingShadowPass()} as a handle taking nothing and giving a boolean, if Iris is here. */
	@Nullable
	private static MethodHandle shadowPassCheck() {
		if (!ModList.get().isLoaded("iris") && !ModList.get().isLoaded("oculus"))
			return null;
		try {
			Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
			Object instance = api.getMethod("getInstance").invoke(null);
			return MethodHandles.publicLookup().findVirtual(api, "isRenderingShadowPass", MethodType.methodType(boolean.class))
				.bindTo(instance);
		} catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
			return null;
		}
	}
}
