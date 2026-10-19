package com.comedymonkey.createaddons.content.engine;

import java.util.List;
import java.util.Locale;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Burns lava from its tank to turn its shaft. Speed and stress capacity are configurable. */
public class MagmaEngineBlockEntity extends GeneratingKineticBlockEntity {
	private static final int TANK_CAPACITY = 4000;

	private final FluidTank tank = new FluidTank(TANK_CAPACITY, stack -> stack.getFluid().isSame(Fluids.LAVA)) {
		@Override
		protected void onContentsChanged() {
			setChanged();
			syncQueued = true;
		}
	};
	/** mB/s accumulated each tick; every 20 is one millibucket burned. */
	private int burnProgress;
	private boolean running;
	private boolean syncQueued;
	/** Client: how far the firebox hatch has swung open, 0 to 1, eased towards open while it burns. */
	private float hatchOpen;
	private float hatchOpenLast;

	public MagmaEngineBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.MAGMA_ENGINE.get(), pos, state);
	}

	public FluidTank getTank() {
		return tank;
	}

	/** Stress capacity per RPM, from the config. */
	public double capacityPerRpm() {
		return CMConfig.SERVER.magmaEngineCapacity();
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
			burnProgress += CMConfig.SERVER.magmaEngineLavaPerSecond();
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

	/** 0 closed to 1 open, smoothed between ticks. */
	public float getHatchOpen(float partialTicks) {
		float t = Mth.lerp(partialTicks, hatchOpenLast, hatchOpen);
		return Mth.sin(t * Mth.HALF_PI);
	}

	@Override
	public float getGeneratedSpeed() {
		if (!running)
			return 0;
		return convertToDirection(CMConfig.SERVER.magmaEngineSpeed(), getBlockState().getValue(MagmaEngineBlock.FACING));
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		LangBuilder lang = new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID);
		lang.translate("gui.goggles.magma_engine").forGoggles(tooltip);
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID)
			.text(String.format(Locale.ROOT, "%,d / %,d mB ", tank.getFluidAmount(), TANK_CAPACITY)).style(ChatFormatting.GOLD)
			.add(new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).add(Fluids.LAVA.getFluidType().getDescription().copy())
				.style(ChatFormatting.GRAY))
			.forGoggles(tooltip, 1);
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID)
			.translate(running ? "gui.goggles.magma_engine.burning" : "gui.goggles.magma_engine.no_fuel",
				CMConfig.SERVER.magmaEngineLavaPerSecond())
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
