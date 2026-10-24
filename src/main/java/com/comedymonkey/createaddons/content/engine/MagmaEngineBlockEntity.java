package com.comedymonkey.createaddons.content.engine;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import org.joml.Quaternionf;

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
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.material.Fluids;
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
	/** Client: run every client tick (the engine's sounds and exhaust puffs); set by the client setup. */
	public static Consumer<MagmaEngineBlockEntity> clientTicker = be -> {};

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

	/** Like vanilla's chest: a wrench turning it moves the sides its pipes and cables connect on, so they look again. */
	@Override
	public void setBlockState(BlockState state) {
		BlockState old = getBlockState();
		super.setBlockState(state);
		if (old.hasProperty(MagmaEngineBlock.FACING) && state.hasProperty(MagmaEngineBlock.FACING)
			&& old.getValue(MagmaEngineBlock.FACING) != state.getValue(MagmaEngineBlock.FACING))
			invalidateCapabilities();
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

	public boolean isSupercharged() {
		return engine().isSupercharged();
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null)
			return;
		if (level.isClientSide) {
			clientTicker.accept(this);
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

	@Override
	public float getGeneratedSpeed() {
		if (!running)
			return 0;
		float speed = convertToDirection(engine().speed(), getBlockState().getValue(MagmaEngineBlock.FACING));
		return direction != null && direction.get() == RotationDirection.COUNTER_CLOCKWISE ? -speed : speed;
	}

	/**
	 * Where the direction setting sits: on its seat on top of the drum at the back, clear of every port (modelled with
	 * the shaft to the north at (8, 13.3, 13), facing up), turned with the engine.
	 */
	private static final class DirectionBox extends ValueBoxTransform {
		@Override
		public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
			return MagmaEngineBlock.modelPoint(state.getValue(MagmaEngineBlock.FACING), 8, 13.35, 13);
		}

		@Override
		public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
			ms.mulPose(new Quaternionf().setFromNormalized(MagmaEngineBlock.modelToWorld(state.getValue(MagmaEngineBlock.FACING))));
			ms.mulPose(Axis.XP.rotationDegrees(90));
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
