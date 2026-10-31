package com.comedymonkey.createaddons.content.wastes;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A dead bot from the Clockwork Wastes (D36): salvage like the broken machines (wrench for scrap and maybe an Electron
 * Tube, anything else for one scrap), held in one of three poses (the {@code pose} state, set by the city). Its model is
 * the approved preview's posed rig baked to quads (models/block/wastes/*_shell_*.obj), so it needs no renderer; a
 * fallen one lies across two blocks and a frozen one stands two tall, but each collides as its own block only.
 * <ul>
 * <li>{@link Worker}: the uprising's dead citizen, slumped against a wall, fallen face down, or frozen mid-step.</li>
 * <li>{@link Resistance}: the clockwork resistance's dead, slumped forward, fallen on its side, or reaching back.</li>
 * </ul>
 */
public abstract class ShellBlock<P extends Enum<P> & StringRepresentable> extends SalvageBlock {
	private final Map<P, Map<Direction, VoxelShape>> shapes;

	protected ShellBlock(Properties properties, Class<P> poses, Map<P, double[]> northBoxes) {
		super(properties);
		Map<P, Map<Direction, VoxelShape>> map = new EnumMap<>(poses);
		northBoxes.forEach((pose, box) -> map.put(pose, shapes(new double[][] { box })));
		this.shapes = map;
	}

	public abstract EnumProperty<P> pose();

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapes.get(state.getValue(pose())).get(state.getValue(FACING));
	}

	public enum WorkerPose implements StringRepresentable {
		SLUMPED, FALLEN, FROZEN;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	public enum ResistancePose implements StringRepresentable {
		SLUMPED, FALLEN, REACHING;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	/** The Worker Shell. Shapes from where each posed model reaches (the exporter prints them), kept to the block. */
	public static class Worker extends ShellBlock<WorkerPose> {
		public static final EnumProperty<WorkerPose> POSE = EnumProperty.create("pose", WorkerPose.class);

		public Worker(Properties properties) {
			super(properties, WorkerPose.class, Map.of(
				WorkerPose.SLUMPED, new double[] { 2, 0, 8, 14, 16, 16 },
				WorkerPose.FALLEN, new double[] { 1, 0, 0, 13, 6, 16 },
				WorkerPose.FROZEN, new double[] { 4, 0, 3, 12, 16, 13 }));
			registerDefaultState(defaultBlockState().setValue(POSE, WorkerPose.SLUMPED));
		}

		@Override
		public EnumProperty<WorkerPose> pose() {
			return POSE;
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			super.createBlockStateDefinition(builder);
			builder.add(POSE);
		}
	}

	/** The Resistance Shell. */
	public static class Resistance extends ShellBlock<ResistancePose> {
		public static final EnumProperty<ResistancePose> POSE = EnumProperty.create("pose", ResistancePose.class);

		public Resistance(Properties properties) {
			super(properties, ResistancePose.class, Map.of(
				ResistancePose.SLUMPED, new double[] { 2, 0, 0, 14, 16, 16 },
				ResistancePose.FALLEN, new double[] { 0, 0, 4, 16, 15, 16 },
				ResistancePose.REACHING, new double[] { 2, 0, 0, 14, 16, 12 }));
			registerDefaultState(defaultBlockState().setValue(POSE, ResistancePose.SLUMPED));
		}

		@Override
		public EnumProperty<ResistancePose> pose() {
			return POSE;
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			super.createBlockStateDefinition(builder);
			builder.add(POSE);
		}
	}
}
