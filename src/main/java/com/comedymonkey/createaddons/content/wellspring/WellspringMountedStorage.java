package com.comedymonkey.createaddons.content.wellspring;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModMountedStorage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.contraption.storage.SyncedMountedStorage;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorage;
import com.simibubi.create.content.contraptions.Contraption;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * A Fluid Wellspring riding on a Create contraption. Blocks on contraptions don't tick, so the tank is carried here
 * instead: it keeps generating (driven by {@link WellspringMovementBehaviour}), contraption pumps, spouts and
 * portable fluid interfaces can use it, it syncs to clients so the visible level rises, and it is written back to
 * the block when the contraption is placed again.
 */
public class WellspringMountedStorage extends MountedFluidStorage implements SyncedMountedStorage {
	private static final Codec<WellspringSource> SOURCE_CODEC = StringRepresentable.fromEnum(WellspringSource::values);

	public static final MapCodec<WellspringMountedStorage> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		Codec.INT.fieldOf("capacity").forGetter(s -> s.tank.getCapacity()),
		FluidStack.OPTIONAL_CODEC.fieldOf("fluid").forGetter(s -> s.tank.getFluid()),
		SOURCE_CODEC.fieldOf("source").forGetter(s -> s.source),
		Codec.INT.fieldOf("generators").forGetter(s -> s.generators),
		Codec.INT.fieldOf("progress").forGetter(s -> s.progress)
	).apply(i, WellspringMountedStorage::new));

	private final FluidTank tank;
	/** What the installed generation upgrades make (NONE without any), and how many there are. */
	private final WellspringSource source;
	private final int generators;
	private int progress;
	private boolean dirty;

	WellspringMountedStorage(int capacity, FluidStack fluid, WellspringSource source, int generators, int progress) {
		super(ModMountedStorage.WELLSPRING.get());
		this.source = source;
		this.generators = generators;
		this.progress = progress;
		this.tank = new FluidTank(capacity, this::isAllowed) {
			@Override
			protected void onContentsChanged() {
				dirty = true;
			}
		};
		tank.setFluid(fluid.copy());
	}

	static WellspringMountedStorage of(WellspringBlockEntity be) {
		return new WellspringMountedStorage(be.getTank().getCapacity(), be.getTank().getFluid(),
			be.getGenerationSource(), be.getGenerationUpgradeCount(), be.getGenerationProgress());
	}

	/** Same rule as the block: water or lava, and only the kind its upgrades make. */
	private boolean isAllowed(FluidStack stack) {
		if (source != WellspringSource.NONE)
			return source.matches(stack);
		return WellspringSource.of(stack) != WellspringSource.NONE;
	}

	/** One tick of generation, at the same configured rate as a placed Wellspring. */
	void generate() {
		int rate = CMConfig.SERVER.generationRate(source, generators);
		if (rate <= 0 || tank.getFluidAmount() >= tank.getCapacity()) {
			progress = 0;
			return;
		}
		progress += rate;
		int made = progress / 20;
		if (made > 0) {
			progress -= made * 20;
			tank.fill(new FluidStack(source.fluid(), made), FluidAction.EXECUTE);
		}
	}

	@Override
	public void unmount(Level level, BlockState state, BlockPos pos, BlockEntity be) {
		if (be instanceof WellspringBlockEntity wellspring) {
			wellspring.getTank().setCapacity(Math.max(wellspring.getTank().getCapacity(), tank.getCapacity()));
			wellspring.getTank().setFluid(tank.getFluid().copy());
			wellspring.setGenerationProgress(progress);
		}
	}

	// ---------------------------------------------------------------- client sync (moving fluid level)

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public void markClean() {
		dirty = false;
	}

	@Override
	public void afterSync(Contraption contraption, BlockPos localPos) {
		if (contraption.getBlockEntityClientSide(localPos) instanceof WellspringBlockEntity wellspring) {
			wellspring.getTank().setCapacity(tank.getCapacity());
			wellspring.getTank().setFluid(tank.getFluid().copy());
		}
	}

	// ---------------------------------------------------------------- IFluidHandler

	@Override
	public int getTanks() {
		return 1;
	}

	@Override
	public FluidStack getFluidInTank(int index) {
		return tank.getFluid();
	}

	@Override
	public int getTankCapacity(int index) {
		return tank.getCapacity();
	}

	@Override
	public boolean isFluidValid(int index, FluidStack stack) {
		return isAllowed(stack);
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		return tank.fill(resource, action);
	}

	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		return tank.drain(resource, action);
	}

	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		return tank.drain(maxDrain, action);
	}
}
