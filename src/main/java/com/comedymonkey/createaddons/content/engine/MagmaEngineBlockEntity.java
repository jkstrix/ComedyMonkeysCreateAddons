package com.comedymonkey.createaddons.content.engine;

import java.util.List;
import java.util.Locale;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity.RotationDirection;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;

import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Burns lava from its tank to turn its shaft. Speed and stress capacity are configurable. */
public class MagmaEngineBlockEntity extends GeneratingKineticBlockEntity {
	private final FluidTank tank;
	/** Which way the shaft turns, set with a wrench-free value box like Simulated's Portable Engine. */
	private ScrollOptionBehaviour<RotationDirection> direction;
	/** mB/s accumulated each tick; every 20 is one millibucket burned. */
	private int burnProgress;
	private boolean running;
	private boolean syncQueued;
	/** Client: how far the firebox hatch has swung open, 0 to 1, eased towards open while it burns. */
	private float hatchOpen;
	private float hatchOpenLast;

	public MagmaEngineBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.MAGMA_ENGINE.get(), pos, state);
		tank = new FluidTank(engine(state).tankCapacity(), stack -> stack.getFluid().isSame(Fluids.LAVA)) {
			@Override
			protected void onContentsChanged() {
				setChanged();
				syncQueued = true;
			}
		};
	}

	/** The engine block: plain or Supercharged, which sets the numbers. */
	private static MagmaEngineBlock engine(BlockState state) {
		return state.getBlock() instanceof MagmaEngineBlock engine ? engine : ModBlocks.MAGMA_ENGINE.get();
	}

	private MagmaEngineBlock engine() {
		return engine(getBlockState());
	}

	public FluidTank getTank() {
		return tank;
	}

	/** Stress capacity per RPM, from the config. */
	public double capacityPerRpm() {
		return engine().capacityPerRpm();
	}

	/** The cylinder heads stand above the block. */
	@Override
	protected AABB createRenderBoundingBox() {
		return new AABB(worldPosition).inflate(0.15, 0, 0).expandTowards(0, 0.4, 0);
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null)
			return;
		if (level.isClientSide) {
			hatchOpenLast = hatchOpen;
			boolean lit = getBlockState().getValue(MagmaEngineBlock.LIT);
			hatchOpen = Mth.clamp(hatchOpen + (lit ? 0.08f : -0.08f), 0, 1);
			return;
		}

		boolean canRun = !tank.isEmpty();
		if (canRun) {
			burnProgress += engine().lavaPerSecond();
			int burn = burnProgress / 20;
			if (burn > 0) {
				burnProgress -= burn * 20;
				tank.drain(burn, FluidTank.FluidAction.EXECUTE);
			}
		}
		if (canRun != running) {
			running = canRun;
			updateGeneratedRotation();
			BlockState state = getBlockState();
			if (state.getValue(MagmaEngineBlock.LIT) != running)
				level.setBlock(worldPosition, state.setValue(MagmaEngineBlock.LIT, running), 3);
		}
		if (syncQueued && level.getGameTime() % 10 == 0) {
			syncQueued = false;
			sendData();
		}
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		direction = new ScrollOptionBehaviour<>(RotationDirection.class,
			Component.translatable("create.contraptions.windmill.rotation_direction"), this, new DirectionBox());
		direction.withCallback(value -> updateGeneratedRotation());
		behaviours.add(direction);
		super.addBehaviours(behaviours);
	}

	/** 0 closed to 1 open, smoothed between ticks. */
	public float getHatchOpen(float partialTicks) {
		float t = Mth.lerp(partialTicks, hatchOpenLast, hatchOpen);
		return Mth.sin(t * Mth.HALF_PI);
	}

	@Override
	public float getGeneratedSpeed() {
		if (!running)
			return 0;
		float speed = convertToDirection(engine().speed(), getBlockState().getValue(MagmaEngineBlock.FACING));
		return direction != null && direction.get() == RotationDirection.COUNTER_CLOCKWISE ? -speed : speed;
	}

	/**
	 * Where the direction setting sits: on top of the body by the hatch, as on Simulated's Portable Engine, when the
	 * engine has that shape; on the back otherwise.
	 */
	private static final class DirectionBox extends ValueBoxTransform {
		private static final boolean SIMULATED_SHAPE = ModList.get().isLoaded("simulated");

		@Override
		public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
			Direction facing = state.getValue(MagmaEngineBlock.FACING);
			// Modelled with the shaft to the north: Simulated's box is a little towards the hatch at the back
			Vec3 local = SIMULATED_SHAPE ? new Vec3(8, 13.5, 8.6) : new Vec3(8, 8, 16.05);
			return rotateAboutY(local.scale(1 / 16.0), facing.toYRot() + 180);
		}

		@Override
		public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
			Direction facing = state.getValue(MagmaEngineBlock.FACING);
			if (SIMULATED_SHAPE) {
				ms.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing.getOpposite())));
				ms.mulPose(Axis.XP.rotationDegrees(90));
			} else {
				ms.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing.getOpposite()) + 180));
			}
		}

		/** Turns a point in the block clockwise (seen from above) about its centre, as a blockstate "y" does. */
		private static Vec3 rotateAboutY(Vec3 v, float degrees) {
			double a = Math.toRadians(degrees);
			double x = v.x - 0.5, z = v.z - 0.5;
			double cos = Math.cos(a), sin = Math.sin(a);
			return new Vec3(0.5 + x * cos - z * sin, v.y, 0.5 + x * sin + z * cos);
		}
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		LangBuilder lang = new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID);
		lang.add(getBlockState().getBlock().getName().copy().append(":")).forGoggles(tooltip);
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID)
			.text(String.format(Locale.ROOT, "%,d / %,d mB ", tank.getFluidAmount(), tank.getCapacity())).style(ChatFormatting.GOLD)
			.add(new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).add(Fluids.LAVA.getFluidType().getDescription().copy())
				.style(ChatFormatting.GRAY))
			.forGoggles(tooltip, 1);
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID)
			.translate(running ? "gui.goggles.magma_engine.burning" : "gui.goggles.magma_engine.no_fuel",
				engine().lavaPerSecond())
			.style(running ? ChatFormatting.AQUA : ChatFormatting.RED)
			.forGoggles(tooltip, 1);
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		return true;
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
		tag.putInt("BurnProgress", burnProgress);
		tag.putBoolean("Running", running);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		tank.setFluid(FluidStack.EMPTY);
		tank.readFromNBT(registries, tag.getCompound("Tank"));
		burnProgress = tag.getInt("BurnProgress");
		running = tag.getBoolean("Running");
	}
}
