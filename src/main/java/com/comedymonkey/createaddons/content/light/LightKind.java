package com.comedymonkey.createaddons.content.light;

import java.util.function.DoubleSupplier;

import com.comedymonkey.createaddons.config.CMConfig;

import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Every Electric Light that is one plain block (design/specs/electric-lights.md): where it mounts, how bright it is at
 * full (P1), what it draws at full (P2, config), and its outline. Shapes are given as built: wall lights on the north
 * wall facing south, ceiling lights hanging from the top of the block, everything else standing on its floor.
 */
public enum LightKind {
	BULKHEAD("bulkhead_light", Mount.WALL, 14, () -> CMConfig.SERVER.lightSmallFePerTick(), false, box(4, 5, 0, 12, 11, 3.5)),
	WALL_SCONCE("wall_sconce", Mount.WALL, 14, () -> CMConfig.SERVER.lightSmallFePerTick(), false, box(5, 4, 0, 11, 12.25, 7.5)),
	GOOSENECK("gooseneck_lamp", Mount.WALL, 14, () -> CMConfig.SERVER.lightSmallFePerTick(), false, box(4.5, 7.5, 0, 11.5, 16, 15.75)),
	HIGH_BAY("high_bay_lamp", Mount.CEILING, 15, () -> CMConfig.SERVER.lightLargeFePerTick(), true, box(1.5, 3, 1.5, 14.5, 16, 14.5)),
	CAGE("cage_lamp", Mount.CEILING, 15, () -> CMConfig.SERVER.lightCeilingFePerTick(), false, box(5, 8.5, 5, 11, 16, 11)),
	FLUSH("flush_ceiling_light", Mount.CEILING, 15, () -> CMConfig.SERVER.lightCeilingFePerTick(), false, box(3, 14.25, 3, 13, 16, 13)),
	BOLLARD("bollard_light", Mount.FLOOR, 13, () -> CMConfig.SERVER.lightSmallFePerTick(), false, box(4.5, 0, 4.5, 11.5, 13, 11.5)),
	FLOOR_LIGHT("floor_light", Mount.FULL, 12, () -> CMConfig.SERVER.lightSmallFePerTick(), false, Shapes.block()),
	INDICATOR("indicator_lamp", Mount.ANY, 4, () -> CMConfig.SERVER.lightIndicatorFePerTick(), false, box(6, 6, 0, 10, 10, 2)),
	LANTERN_HEAD("lantern_head", Mount.TOP, 15, () -> CMConfig.SERVER.lightLargeFePerTick(), false, box(4.5, 0, 4.5, 11.5, 15, 11.5)),
	COBRA_HEAD("cobra_head", Mount.TOP_FACING, 15, () -> CMConfig.SERVER.lightLargeFePerTick(), false, box(5.5, 0, -6.5, 10.5, 10, 9.5)),
	GLOBE_HEAD("globe_head", Mount.TOP, 15, () -> CMConfig.SERVER.lightLargeFePerTick(), false, box(4, 0, 4, 12, 11, 12)),
	YARD_HEAD("yard_light_head", Mount.TOP_FACING, 15, () -> CMConfig.SERVER.lightLargeFePerTick(), true, box(2, 0, 1, 14, 8, 13)),
	/** Hung under a ceiling, lowered on cable (HangingLampBlock). */
	PENDANT("pendant_lamp", Mount.CEILING, 15, () -> CMConfig.SERVER.lightCeilingFePerTick(), false, box(4.5, 4.5, 4.5, 11.5, 16, 11.5)),
	/** Hung like the Pendant; brass, several arms, twice its draw (P2). */
	CHANDELIER("chandelier", Mount.CEILING, 15, () -> CMConfig.SERVER.lightCeilingFePerTick() * 2, false, box(1, 3.5, 1, 15, 16, 15)),
	/** On any face; its rotor turns while it's lit (LightRenderer). Built on the north wall facing south, as the Indicator. */
	WARNING_BEACON("warning_beacon", Mount.ANY, 10, () -> CMConfig.SERVER.lightBeaconFePerTick(), false, box(4.5, 4.5, 0, 11.5, 11.5, 8));

	/** Where a light is fixed, and so which ways it can face. */
	public enum Mount {
		/** On the side of a block, facing out of it (any horizontal way). */
		WALL,
		/** Under a block, hanging. */
		CEILING,
		/** On top of a block, the same from every side. */
		FLOOR,
		/** A whole block, set into a floor. */
		FULL,
		/** On any face of a block, facing out of it. */
		ANY,
		/** On top of any block, the same from every side (a lamp head). */
		TOP,
		/** On top of any block, facing the way it was placed (a lamp head that leans out). */
		TOP_FACING;

		/** It faces one of the four horizontal ways. */
		public boolean horizontal() {
			return this == WALL || this == TOP_FACING;
		}

		/** It faces any of the six ways. */
		public boolean anyWay() {
			return this == ANY;
		}
	}

	/** The brightness steps the wrench goes through (D22): Full, Bright, Dim, Glow, as a share of full. */
	public static final double[] STEPS = {1.0, 0.8, 0.5, 0.25};

	private final String id;
	private final Mount mount;
	private final int level;
	private final DoubleSupplier draw;
	private final boolean warms;
	private final VoxelShape shape;

	LightKind(String id, Mount mount, int level, DoubleSupplier draw, boolean warms, VoxelShape shape) {
		this.id = id;
		this.mount = mount;
		this.level = level;
		this.draw = draw;
		this.warms = warms;
		this.shape = shape;
	}

	public String id() {
		return id;
	}

	public Mount mount() {
		return mount;
	}

	/** Light level at full brightness. */
	public int level() {
		return level;
	}

	/** Light level at a brightness step: at least 1, so a dimmed light still lights. */
	public int level(int step) {
		return Math.max(1, (int) Math.round(level * STEPS[step]));
	}

	/** FE a tick at a brightness step. */
	public double draw(int step) {
		return draw.getAsDouble() * STEPS[step];
	}

	/** It warms up like a sodium lamp (P4): the High-Bay and the Yard Light. */
	public boolean warms() {
		return warms;
	}

	/** Its outline, as built (before turning to face its way). */
	public VoxelShape shape() {
		return shape;
	}

	private static VoxelShape box(double x0, double y0, double z0, double x1, double y1, double z1) {
		return Shapes.box(x0 / 16, y0 / 16, Math.max(0, z0) / 16, x1 / 16, y1 / 16, Math.min(16, z1) / 16);
	}
}
