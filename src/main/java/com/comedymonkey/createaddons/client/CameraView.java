package com.comedymonkey.createaddons.client;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;

/**
 * Where the camera is from a block that's being drawn, in the block's own coordinates (0 to 1 across it). Worked out
 * from the pose, whose origin is the camera, so it's right on contraptions and ships too. Not known in Ponder, whose
 * pose starts somewhere else, nor in a shader pack's shadow pass, which draws from the sun: then everything is drawn.
 */
final class CameraView {
	/** The glass of every glass-fronted machine is set this far in from its front face (1.2 px), less a little to spare. */
	private static final double GLASS_INSET = 1.2 / 16 - 0.25 / 16;
	/** Only ever used on the render thread. */
	private static final Matrix4f INVERSE = new Matrix4f();
	/** Iris's (or Oculus's) "is it drawing the shadow pass" check, if it's installed. */
	@Nullable
	private static final MethodHandle SHADOW_PASS = shadowPassCheck();
	@Nullable
	private static final Object IRIS = SHADOW_PASS == null ? null : irisApi();

	private final double x, y, z;
	private final boolean known;

	private CameraView(double x, double y, double z, boolean known) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.known = known;
	}

	/** Call with the pose as the renderer was given it, before it's moved. */
	static CameraView of(BlockEntity be, PoseStack ms) {
		if (be.getLevel() instanceof PonderLevel || inShadowPass())
			return new CameraView(0.5, 0.5, 0.5, false);
		ms.last().pose().invertAffine(INVERSE);
		return new CameraView(INVERSE.m30(), INVERSE.m31(), INVERSE.m32(), true);
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

	private static boolean inShadowPass() {
		if (SHADOW_PASS == null || IRIS == null)
			return false;
		try {
			return (boolean) SHADOW_PASS.invoke(IRIS);
		} catch (Throwable e) {
			return false;
		}
	}

	@Nullable
	private static MethodHandle shadowPassCheck() {
		if (!ModList.get().isLoaded("iris") && !ModList.get().isLoaded("oculus"))
			return null;
		try {
			Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
			return MethodHandles.publicLookup().findVirtual(api, "isRenderingShadowPass", MethodType.methodType(boolean.class));
		} catch (ReflectiveOperationException | LinkageError e) {
			return null;
		}
	}

	@Nullable
	private static Object irisApi() {
		try {
			Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
			return api.getMethod("getInstance").invoke(null);
		} catch (ReflectiveOperationException | LinkageError e) {
			return null;
		}
	}
}
