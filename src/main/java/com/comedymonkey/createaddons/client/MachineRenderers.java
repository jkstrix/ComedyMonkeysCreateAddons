package com.comedymonkey.createaddons.client;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.parts.PartSink;
import com.comedymonkey.createaddons.content.machine.TwoInputMachineBlock;
import com.comedymonkey.createaddons.content.machine.TwoInputMachineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * What goes on inside the String Tangler and the Stone Infuser, both modelled with the window facing north and
 * the shaft going into the south face. Everything is drawn into one cutout buffer.
 */
public final class MachineRenderers {
	private static final BlockSprite COBBLESTONE = new BlockSprite(ResourceLocation.withDefaultNamespace("block/cobblestone"));

	private MachineRenderers() {}

	/** Turns the pose so the model's north is the machine's front, about the block centre. */
	private static void faceFront(PoseStack ms, Direction front) {
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-(front.toYRot() + 180)));
		ms.translate(-0.5, -0.5, -0.5);
	}

	/** The shaft stub in the back, spun about the world axis like the shaft it meets. */
	private static void shaft(TwoInputMachineBlockEntity be, Direction front, PoseStack ms, PartSink sink, int light) {
		Direction back = front.getOpposite();
		Direction.Axis axis = back.getAxis();
		float angle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), axis);
		ms.pushPose();
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(axis == Direction.Axis.X ? Axis.XP.rotation(angle) : Axis.ZP.rotation(angle));
		ms.mulPose(Axis.YP.rotationDegrees(-(back.toYRot() + 180)));
		ms.translate(-0.5, -0.5, -0.5);
		sink.part(CobbleGeneratorRenderer.SHAFT, ms, light);
		ms.popPose();
	}

	/**
	 * A flat quad between four points a, b, c, d (in block units), both sides, with a sprite's u0..u1 v0..v1 (0-16):
	 * a at (u0, v0), b at (u0, v1), c at (u1, v1), d at (u1, v0).
	 */
	private static void quad(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite s, int light, int rgb,
							 float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
							 float dx, float dy, float dz, float u0, float v0, float u1, float v1) {
		float ua = s.getU(u0 / 16f), ub = s.getU(u1 / 16f), va = s.getV(v0 / 16f), vb = s.getV(v1 / 16f);
		vertex(vc, pose, light, rgb, ax, ay, az, ua, va);
		vertex(vc, pose, light, rgb, bx, by, bz, ua, vb);
		vertex(vc, pose, light, rgb, cx, cy, cz, ub, vb);
		vertex(vc, pose, light, rgb, dx, dy, dz, ub, va);
		// and the back, the other way round
		vertex(vc, pose, light, rgb, dx, dy, dz, ub, va);
		vertex(vc, pose, light, rgb, cx, cy, cz, ub, vb);
		vertex(vc, pose, light, rgb, bx, by, bz, ua, vb);
		vertex(vc, pose, light, rgb, ax, ay, az, ua, va);
	}

	private static void vertex(VertexConsumer vc, PoseStack.Pose pose, int light, int rgb, float x, float y, float z,
							   float u, float v) {
		vc.addVertex(pose, x, y, z)
			.setColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255)
			.setUv(u, v)
			.setLight(light)
			.setNormal(pose, 0, 1, 0);
	}

	/** An axis-aligned box in pixels, every face, textured from the matching part of the sprite. */
	private static void cube(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite s, int light,
							 float x0, float y0, float z0, float x1, float y1, float z1) {
		float a = x0 / 16, b = y0 / 16, c = z0 / 16, d = x1 / 16, e = y1 / 16, f = z1 / 16;
		int white = 0xFFFFFF;
		quad(vc, pose, s, light, white, a, e, c, a, e, f, d, e, f, d, e, c, x0, z0, x1, z1);           // up
		quad(vc, pose, s, light, white, a, b, c, d, b, c, d, b, f, a, b, f, x0, z0, x1, z1);           // down
		quad(vc, pose, s, light, white, d, e, c, d, b, c, a, b, c, a, e, c, 16 - x1, 16 - y1, 16 - x0, 16 - y0); // north
		quad(vc, pose, s, light, white, a, e, f, a, b, f, d, b, f, d, e, f, x0, 16 - y1, x1, 16 - y0); // south
		quad(vc, pose, s, light, white, a, e, c, a, b, c, a, b, f, a, e, f, z0, 16 - y1, z1, 16 - y0); // west
		quad(vc, pose, s, light, white, d, e, f, d, b, f, d, b, c, d, e, c, 16 - z1, 16 - y1, 16 - z0, 16 - y0); // east
	}

	// ---------------------------------------------------------------- parts

	private static ModelResourceLocation part(String name) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/" + name));
	}

	private static final ModelResourceLocation WHISK = part("string_tangler_whisk");
	private static final ModelResourceLocation SPOOL_WEST = part("string_tangler_spool_west");
	private static final ModelResourceLocation SPOOL_EAST = part("string_tangler_spool_east");
	private static final ModelResourceLocation WINDING_WEST = part("string_tangler_winding_west");
	private static final ModelResourceLocation WINDING_EAST = part("string_tangler_winding_east");
	private static final ModelResourceLocation TURNTABLE = part("stone_infuser_turntable");
	private static final ModelResourceLocation DRIVE = part("stone_infuser_drive");

	/** The machines' moving parts aren't referenced by any blockstate, so they're loaded explicitly. */
	public static void registerModels(ModelEvent.RegisterAdditional event) {
		for (ModelResourceLocation m : new ModelResourceLocation[] {WHISK, SPOOL_WEST, SPOOL_EAST, WINDING_WEST,
			WINDING_EAST, TURNTABLE, DRIVE})
			event.register(m);
	}

	/**
	 * The shaft's turn about the model's +Z (out of the back), matching the stub: the stub spins about the world axis,
	 * which runs the other way in model space when the back faces a negative direction.
	 */
	private static float modelSpin(TwoInputMachineBlockEntity be, Direction front) {
		Direction back = front.getOpposite();
		float angle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), back.getAxis());
		return back.getAxisDirection() == Direction.AxisDirection.POSITIVE ? angle : -angle;
	}

	/** Spins whatever is drawn next about a line through (x, y, z) pixels, along the given axis. */
	private static void spinAbout(PoseStack ms, float x, float y, float z, Axis axis, float radians) {
		ms.translate(x / 16, y / 16, z / 16);
		ms.mulPose(axis.rotation(radians));
		ms.translate(-x / 16, -y / 16, -z / 16);
	}

	// ---------------------------------------------------------------- String Tangler

	/**
	 * The String Tangler (design/briefs/string-tangler.md): a Create whisk on the shaft's axis, turning with it while
	 * it works; two spools of string at the side walls whose winding shows how much string is loaded, paying out to
	 * the whisk; the slime in its pot, by how much there is; the web balling up on the whisk as the job goes on; and
	 * the finished cobwebs in the tray at the front. Idle, the whisk is still and the strands hang slack.
	 */
	public static class Tangler extends SafeBlockEntityRenderer<TwoInputMachineBlockEntity> {
		private static final BlockSprite STRING = new BlockSprite(ComedyMonkeysCreateAddons.asResource("block/string_tangler_string"));
		private static final BlockSprite SLIME = new BlockSprite(ComedyMonkeysCreateAddons.asResource("block/string_tangler_slime"));
		private static final BlockSprite WEB = new BlockSprite(ComedyMonkeysCreateAddons.asResource("block/string_tangler_web"));
		/** The whisk's axis, the spools' centres and the inner faces of their windings, in model pixels. */
		private static final float AXIS_X = 8, AXIS_Y = 8, SPOOL_Z = 8, WEST_X = 3.5f, EAST_X = 12.5f, WINDING_HALF = 0.75f;
		/** The tray's floor (the harvester's tray, rebuilt here), in pixels. */
		private static final float TRAY_Y = 2.5f;

		public Tangler(BlockEntityRendererProvider.Context context) {}

		@Override
		protected void renderSafe(TwoInputMachineBlockEntity be, float partialTicks, PoseStack ms,
								  MultiBufferSource buffer, int light, int overlay) {
			BlockState state = be.getBlockState();
			if (!state.hasProperty(TwoInputMachineBlock.FACING))
				return;
			parts(be, partialTicks, ms, PartSink.forRenderer(be, buffer, overlay), light);
			Direction front = state.getValue(TwoInputMachineBlock.FACING);
			VertexConsumer vc = buffer.getBuffer(RenderType.cutout());

			ms.pushPose();
			faceFront(ms, front);
			boolean working = be.isProcessing();
			float spin = working ? modelSpin(be, front) : 0;
			float progress = working ? be.getProgress(partialTicks) : 0;
			ItemStack slime = be.getStack(TwoInputMachineBlockEntity.EXTRA);

			// The web balling up on the whisk, turning with it
			if (progress > 0.02f) {
				ms.pushPose();
				spinAbout(ms, AXIS_X, AXIS_Y, 0, Axis.ZP, spin);
				web(vc, ms.last(), WEB.get(), light, 3 * progress);
				ms.popPose();
			}

			// Two strands from each spool's winding to the whisk, wound on where they meet it
			float wound = wound(be.getStack(TwoInputMachineBlockEntity.MAIN));
			if (wound > 0)
				for (int side = 0; side < 2; side++) {
					boolean west = side == 0;
					float x = west ? WEST_X : EAST_X;
					float from = west ? x + WINDING_HALF : x - WINDING_HALF;
					float r = 1.25f * wound;
					for (int i = 0; i < 2; i++) {
						float at = spin + (west ? 0 : Mth.PI) + i * 0.6f;
						float sag = working ? 0 : 0.5f;
						strand(vc, ms.last(), STRING.get(), light, from / 16, (AXIS_Y + (i == 0 ? r : -r) * 0.6f) / 16,
							(AXIS_X + Mth.cos(at) * 1.5f) / 16, (AXIS_Y + Mth.sin(at) * 1.5f - sag) / 16, (SPOOL_Z - 0.5f + i) / 16);
					}
				}

			// The slime in its pot, a blob that shrinks as it's used
			if (!slime.isEmpty()) {
				float h = 0.4f + 1.1f * slime.getCount() / slime.getMaxStackSize();
				cube(vc, ms.last(), SLIME.get(), light, 7, 2.25f, 5, 9, 2.25f + h, 7);
			}

			TrayItems.render(be.getStack(TwoInputMachineBlockEntity.OUTPUT), TRAY_Y, be.getLevel(), ms, buffer, light, overlay);
			ms.popPose();
		}

		/** The part models: the shaft end, the whisk, the spools and their windings (for this renderer, or Flywheel's visual). */
		public static void parts(TwoInputMachineBlockEntity be, float partialTicks, PoseStack ms, PartSink sink, int light) {
			BlockState state = be.getBlockState();
			if (!state.hasProperty(TwoInputMachineBlock.FACING))
				return;
			Direction front = state.getValue(TwoInputMachineBlock.FACING);
			shaft(be, front, ms, sink, light);
			ms.pushPose();
			faceFront(ms, front);
			float spin = be.isProcessing() ? modelSpin(be, front) : 0;

			// The whisk, turning with the shaft
			ms.pushPose();
			spinAbout(ms, AXIS_X, AXIS_Y, 0, Axis.ZP, spin);
			sink.part(WHISK, ms, light);
			ms.popPose();

			// The spools pay out slower than the whisk turns; their winding shows how much string is left
			float wound = wound(be.getStack(TwoInputMachineBlockEntity.MAIN));
			for (int side = 0; side < 2; side++) {
				boolean west = side == 0;
				ms.pushPose();
				spinAbout(ms, west ? WEST_X : EAST_X, AXIS_Y, SPOOL_Z, Axis.XP, (west ? 1 : -1) * spin / 6);
				sink.part(west ? SPOOL_WEST : SPOOL_EAST, ms, light);
				if (wound > 0) {
					ms.translate(0, AXIS_Y / 16, SPOOL_Z / 16);
					ms.scale(1, wound, wound);
					ms.translate(0, -AXIS_Y / 16, -SPOOL_Z / 16);
					sink.part(west ? WINDING_WEST : WINDING_EAST, ms, light);
				}
				ms.popPose();
			}
			ms.popPose();
		}

		/** How full the windings are drawn: none without string, else from 45 % to full with the stack. */
		private static float wound(ItemStack string) {
			return string.isEmpty() ? 0 : 0.45f + 0.55f * string.getCount() / string.getMaxStackSize();
		}

		/** Three crossed planes of web round the whisk's middle, {@code half} pixels out from the axis. */
		private static void web(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite s, int light, float half) {
			float x0 = (AXIS_X - half) / 16, x1 = (AXIS_X + half) / 16, y0 = (AXIS_Y - half) / 16, y1 = (AXIS_Y + half) / 16;
			float z0 = (8 - half) / 16, z1 = (8 + half) / 16, cx = AXIS_X / 16, cy = AXIS_Y / 16, cz = 0.5f;
			int tint = 0xFFFFFF;
			quad(vc, pose, s, light, tint, x0, y1, cz, x0, y0, cz, x1, y0, cz, x1, y1, cz, 0, 0, 16, 16);
			quad(vc, pose, s, light, tint, cx, y1, z0, cx, y0, z0, cx, y0, z1, cx, y1, z1, 0, 0, 16, 16);
			quad(vc, pose, s, light, tint, x0, cy, z0, x0, cy, z1, x1, cy, z1, x1, cy, z0, 0, 0, 16, 16);
		}

		/**
		 * A ribbon of string from (fromX, fromY) to (toX, toY), half a pixel wide, turned to face the front, at depth
		 * {@code z} (all in blocks).
		 */
		private static void strand(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite s, int light,
								   float fromX, float fromY, float toX, float toY, float z) {
			float dx = toX - fromX, dy = toY - fromY;
			float len = Mth.sqrt(dx * dx + dy * dy);
			float w = 0.25f / 16f;
			float nx = len < 1e-4f ? w : -dy / len * w, ny = len < 1e-4f ? 0 : dx / len * w;
			quad(vc, pose, s, light, 0xFFFFFF, fromX + nx, fromY + ny, z, fromX - nx, fromY - ny, z, toX - nx, toY - ny, z,
				toX + nx, toY + ny, z, 0, 7, 16, 9);
		}
	}

	// ---------------------------------------------------------------- Stone Infuser

	/**
	 * The Stone Infuser (design/briefs/stone-infuser.md): a turntable geared down 4:1 from the shaft through a drive
	 * rod, so the cobblestone on it turns at the speed it's given; the new stone climbing it from the bottom as the
	 * job goes on; the nuggets piled in the feeder, by how many there are (the dust falling from its spout is the
	 * block entity's); and finished stones in the tray at the front.
	 */
	public static class Infuser extends SafeBlockEntityRenderer<TwoInputMachineBlockEntity> {
		/** The turntable's axis and the stone on it, in model pixels (set forward so its corners clear the stub). */
		private static final float TABLE_X = 8, TABLE_Z = 7.75f, DRIVE_Y = 2.375f;
		private static final float STONE_X0 = 5, STONE_X1 = 11, STONE_Y0 = 3.5f, STONE_Y1 = 9.5f, STONE_Z0 = 4.75f, STONE_Z1 = 10.75f;
		private static final float TRAY_Y = 2.5f;

		public Infuser(BlockEntityRendererProvider.Context context) {}

		@Override
		protected void renderSafe(TwoInputMachineBlockEntity be, float partialTicks, PoseStack ms,
								  MultiBufferSource buffer, int light, int overlay) {
			BlockState state = be.getBlockState();
			if (!state.hasProperty(TwoInputMachineBlock.FACING))
				return;
			Direction front = state.getValue(TwoInputMachineBlock.FACING);
			boolean behind = CameraView.of(be, ms).behindGlass(front);
			parts(be, ms, PartSink.forRenderer(be, buffer, overlay), light, !behind);
			// Everything else is inside, seen only through the front glass
			if (behind)
				return;
			VertexConsumer vc = buffer.getBuffer(RenderType.cutout());

			ms.pushPose();
			faceFront(ms, front);
			float spin = modelSpin(be, front);

			// The cobblestone on the turntable, turning with it
			boolean working = be.isProcessing() && !be.getMaking().isEmpty();
			if (working || !be.getStack(TwoInputMachineBlockEntity.MAIN).isEmpty()) {
				ms.pushPose();
				spinAbout(ms, TABLE_X, 0, TABLE_Z, Axis.YP, spin / 4);
				PoseStack.Pose pose = ms.last();
				cube(vc, pose, COBBLESTONE.get(), light, STONE_X0, STONE_Y0, STONE_Z0, STONE_X1, STONE_Y1, STONE_Z1);
				// The new stone climbing it, stopping just short of the top so their tops never meet
				float rise = working ? (STONE_Y1 - STONE_Y0 - 0.1f) * be.getProgress(partialTicks) : 0;
				if (rise > 0.05f)
					cube(vc, pose, blockSprite(be.getMaking()), light, STONE_X0 - 0.1f, STONE_Y0 - 0.1f, STONE_Z0 - 0.1f,
						STONE_X1 + 0.1f, STONE_Y0 + rise, STONE_Z1 + 0.1f);
				ms.popPose();
			}

			// The nuggets lying in the feeder: one to four, by how many are loaded
			ItemStack nuggets = be.getStack(TwoInputMachineBlockEntity.EXTRA);
			if (!nuggets.isEmpty()) {
				int shown = Math.min(4, 1 + nuggets.getCount() / 16);
				for (int i = 0; i < shown; i++) {
					ms.pushPose();
					ms.translate((3 + (i % 2) * 1.25f) / 16, (10.35f + i * 0.02f) / 16, (6.75f + (i / 2) * 1.5f) / 16);
					ms.mulPose(Axis.YP.rotationDegrees(40 * i));
					ms.mulPose(Axis.XP.rotationDegrees(90));
					ms.scale(0.22f, 0.22f, 0.22f);
					Minecraft.getInstance().getItemRenderer().renderStatic(nuggets, ItemDisplayContext.FIXED, light, overlay,
						ms, buffer, be.getLevel(), i);
					ms.popPose();
				}
			}

			TrayItems.render(be.getStack(TwoInputMachineBlockEntity.OUTPUT), TRAY_Y, be.getLevel(), ms, buffer, light, overlay);
			ms.popPose();
		}

		/** The part models: the shaft end, the drive rod and the turntable (for this renderer, or Flywheel's visual). */
		public static void parts(TwoInputMachineBlockEntity be, float partialTicks, PoseStack ms, PartSink sink, int light) {
			parts(be, ms, sink, light, true);
		}

		/** The drive and the turntable are inside, left out when they can't be seen ({@code inside} false). */
		private static void parts(TwoInputMachineBlockEntity be, PoseStack ms, PartSink sink, int light, boolean inside) {
			BlockState state = be.getBlockState();
			if (!state.hasProperty(TwoInputMachineBlock.FACING))
				return;
			Direction front = state.getValue(TwoInputMachineBlock.FACING);
			shaft(be, front, ms, sink, light);
			if (!inside)
				return;
			ms.pushPose();
			faceFront(ms, front);
			float spin = modelSpin(be, front);
			// The drive rod turns with the shaft; the turntable geared down 4:1 under the stone
			ms.pushPose();
			spinAbout(ms, TABLE_X, DRIVE_Y, 0, Axis.ZP, spin);
			sink.part(DRIVE, ms, light);
			ms.popPose();
			ms.pushPose();
			spinAbout(ms, TABLE_X, 0, TABLE_Z, Axis.YP, spin / 4);
			sink.part(TURNTABLE, ms, light);
			ms.popPose();
			ms.popPose();
		}

		/** The particle (side) sprite of a block item's block, or cobblestone if it isn't one. */
		private static TextureAtlasSprite blockSprite(ItemStack stack) {
			if (stack.getItem() instanceof BlockItem item)
				return Minecraft.getInstance().getBlockRenderer().getBlockModelShaper()
					.getParticleIcon(item.getBlock().defaultBlockState());
			return COBBLESTONE.get();
		}
	}
}
