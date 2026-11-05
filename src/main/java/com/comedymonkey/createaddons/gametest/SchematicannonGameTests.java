package com.comedymonkey.createaddons.gametest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.light.ArmFinish;
import com.comedymonkey.createaddons.content.light.LampHeadBlock;
import com.comedymonkey.createaddons.content.light.LightBlock;
import com.comedymonkey.createaddons.content.light.LightBlockEntity;
import com.comedymonkey.createaddons.content.light.LightKind;
import com.comedymonkey.createaddons.content.line.Fitting;
import com.comedymonkey.createaddons.content.line.LineBlockEntity;
import com.comedymonkey.createaddons.content.line.LinePostBlock;
import com.comedymonkey.createaddons.content.line.LinePostBlockEntity;
import com.comedymonkey.createaddons.content.line.LineTier;
import com.comedymonkey.createaddons.content.line.Span;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModDataComponents;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.SchematicProcessor;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.utility.BlockHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Builds printed by a Create Schematicannon. {@link #print} copies a box of the test world the way the cannon does: a
 * layer at a time from the bottom, row by row, brittle blocks held back to the end, one block every couple of ticks, each
 * placed with only its safe data (./gradlew runGameTestServer).
 */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SchematicannonGameTests {
	private static final String COURSE = "trade_bot_course";
	/** Ticks between two blocks: the cannon's shortest delay is a little longer, and its blocks fly a while too. */
	private static final int SHOT = 2;

	private SchematicannonGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(SchematicannonGameTests.class);
	}

	/**
	 * At tick {@code start}, prints the box {@code min}..{@code max} again with its low corner at {@code to}, unturned;
	 * {@code check} runs once the last block has had 150 ticks to settle.
	 */
	static void print(GameTestHelper helper, BlockPos min, BlockPos max, BlockPos to, int start, Runnable check) {
		print(helper, min, max, to, start, Rotation.NONE, Mirror.NONE, new ArrayList<>(), check);
	}

	/** The cannon's placement settings for a schematic turned and mirrored so. */
	static StructurePlaceSettings settings(Rotation rotation, Mirror mirror) {
		return new StructurePlaceSettings().setRotation(rotation).setMirror(mirror);
	}

	/** Where the block at {@code pos} in the box from {@code min} lands, printed at {@code to} turned and mirrored so. */
	static BlockPos landed(BlockPos min, BlockPos to, BlockPos pos, Rotation rotation, Mirror mirror) {
		return to.offset(StructureTemplate.calculateRelativePosition(settings(rotation, mirror), pos.subtract(min)));
	}

	/**
	 * Prints the box as a Schematicannon prints a schematic of it: saved as a structure, laid out in Create's schematic
	 * level (turned and mirrored so, with Create's own processing and block entity transform), then a block every
	 * {@link #SHOT} ticks in the cannon's order, each costed and placed by Create's own code from its safe data. What the
	 * cannon would spend goes in {@code spent}.
	 */
	static void print(GameTestHelper helper, BlockPos min, BlockPos max, BlockPos to, int start, Rotation rotation, Mirror mirror,
					  List<ItemRequirement.StackRequirement> spent, Runnable check) {
		// (every tick is booked now: the game test can't take new ones while it's running them. The box can't hold more
		// blocks than it has places, so that many shots are booked, each printing the next block if there is one)
		int places = (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
		List<Runnable> shots = new ArrayList<>();
		helper.runAtTickTime(start, () -> {
			ServerLevel level = helper.getLevel();
			StructureTemplate template = new StructureTemplate();
			template.fillFromWorld(level, helper.absolutePos(min), max.subtract(min).offset(1, 1, 1), false, Blocks.AIR);
			StructurePlaceSettings settings = settings(rotation, mirror).addProcessor(SchematicProcessor.INSTANCE);
			BlockPos anchor = helper.absolutePos(to);
			SchematicLevel reader = new SchematicLevel(anchor, level);
			template.placeInWorld(reader, anchor, anchor, settings, reader.getRandom(), Block.UPDATE_CLIENTS);
			StructureTransform transform = new StructureTransform(settings.getRotationPivot(), Direction.Axis.Y,
				settings.getRotation(), settings.getMirror());
			for (BlockEntity be : reader.getBlockEntities())
				transform.apply(be);
			// a layer at a time from the bottom, row by row (z), along each row (x); brittle blocks last
			List<BlockPos> order = new ArrayList<>(reader.getAllPositions()), deferred = new ArrayList<>();
			order.sort(Comparator.<BlockPos>comparingInt(p -> p.getY()).thenComparingInt(p -> p.getZ()).thenComparingInt(p -> p.getX()));
			order.removeIf(p -> {
				BlockState state = reader.getBlockState(anchor.offset(p));
				if (state.isAir())
					return true;
				if (SchematicPrinter.shouldDeferBlock(state)) {
					deferred.add(p);
					return true;
				}
				return false;
			});
			order.addAll(deferred);
			for (BlockPos rel : order) {
				BlockPos target = anchor.offset(rel);
				shots.add(() -> {
					BlockState state = BlockHelper.setZeroAge(reader.getBlockState(target));
					if (level.getBlockState(target) == state)
						return;
					CompoundTag data = BlockHelper.prepareBlockEntityData(reader, state, reader.getBlockEntity(target));
					// the cost, read as SchematicPrinter.getCurrentRequirement reads it
					BlockEntity costed = state.getBlock() instanceof EntityBlock e ? e.newBlockEntity(target, state) : null;
					if (costed != null && data != null)
						costed.loadWithComponents(data.copy(), reader.registryAccess());
					var cost = ItemRequirement.of(state, costed).getRequiredItems();
					// the cannon checks each charge on its own before taking any: two that one stack meets both pass on it
					for (int i = 0; i < cost.size(); i++)
						for (int j = i + 1; j < cost.size(); j++)
							if (cost.get(i).matches(cost.get(j).stack) || cost.get(j).matches(cost.get(i).stack))
								helper.fail("printing " + state + " charges " + cost.get(i).stack + " and " + cost.get(j).stack
									+ " apart: one stack would pay for both");
					spent.addAll(cost);
					ItemStack icon = cost.isEmpty() ? ItemStack.EMPTY : cost.get(0).stack;
					BlockHelper.placeSchematicBlock(level, state, target, icon, data == null ? null : data.copy());
				});
			}
		});
		for (int i = 0; i < places; i++) {
			int shot = i;
			helper.runAtTickTime(start + 1 + (long) i * SHOT, () -> {
				if (shot < shots.size())
					shots.get(shot).run();
			});
		}
		helper.runAtTickTime(start + 1 + (long) places * SHOT + 150, check);
	}

	/**
	 * A printed Lamp Post, fed by a printed wire at its foot, lights its Lantern Head on top and the Cobra Head on its arm,
	 * as the one it was copied from does.
	 */
	@GameTest(template = COURSE, timeoutTicks = 260)
	public static void printedLampPostLights(GameTestHelper helper) {
		// the copy: two wires to a three-high Cast Iron Lamp Post (on stone), a Lantern Head on top, a Cobra Head on its arm
		BlockPos foot = new BlockPos(4, 1, 2), head = foot.above(2);
		for (int i = 1; i <= 2; i++)
			floorWire(helper, foot.west(i));
		helper.setBlock(foot.below(), Blocks.STONE);
		for (int i = 0; i < 3; i++)
			helper.setBlock(foot.above(i), ModBlocks.CAST_IRON_LAMP_POST.get());
		helper.runAtTickTime(3, () -> {
			helper.setBlock(head.above(), ModBlocks.LIGHTS.get(LightKind.LANTERN_HEAD).get());
			helper.setBlock(head.north(), ModBlocks.LIGHTS.get(LightKind.COBRA_HEAD).get().defaultBlockState()
				.setValue(LampHeadBlock.ARM, ArmFinish.IRON).setValue(LightBlock.FACING, Direction.NORTH));
			LineGameTests.relink(helper, foot.west(1), foot.west(2));
		});
		// the print, eight blocks south, fed by a generator put there by hand
		BlockPos shift = new BlockPos(0, 0, 8), min = new BlockPos(2, 0, 1), max = new BlockPos(4, 4, 2);
		LineGameTests.generator(helper, foot.west(3).offset(shift));
				BlockPos printed = head.offset(shift);
		print(helper, min, max, min.offset(shift), 8, () -> {
			if (!(helper.getLevel().getBlockEntity(helper.absolutePos(printed.above())) instanceof LightBlockEntity top) || top.phase() != LightBlockEntity.Phase.LIT)
				{
					StringBuilder sb = new StringBuilder();
					for (int i = 1; i <= 2; i++)
						if (helper.getLevel().getBlockEntity(helper.absolutePos(foot.west(i).offset(shift))) instanceof com.comedymonkey.createaddons.content.line.PowerLineBlockEntity w)
							sb.append(" wire").append(i).append(" W=").append(w.link(Direction.DOWN, Direction.WEST)).append(" E=").append(w.link(Direction.DOWN, Direction.EAST));
					for (int i = 0; i < 3; i++)
						sb.append(" post").append(i).append("=").append(helper.getBlockState(foot.above(i).offset(shift)));
					helper.fail("the printed Lantern Head on top of the pole isn't lit: " + describe(helper, printed.above()) + sb);
				}
			if (!(helper.getLevel().getBlockEntity(helper.absolutePos(printed.north())) instanceof LightBlockEntity arm) || arm.phase() != LightBlockEntity.Phase.LIT)
				helper.fail("the printed Cobra Head on the arm isn't lit: " + describe(helper, printed.north()));
			helper.succeed();
		});
	}

	/**
	 * A printed Chandelier hangs on its two blocks of cable under its ceiling, as the one it was copied from does, and
	 * lights from a printed wire on its ceiling block's side.
	 */
	@GameTest(template = COURSE, timeoutTicks = 260)
	public static void printedChandelierHangsAndLights(GameTestHelper helper) {
		// the copy: a wire on the side of a ceiling block, a Chandelier lowered twice under it
		BlockPos gen = new BlockPos(1, 4, 1), w = gen.east(), ceiling = w.south(), lamp = ceiling.below();
		helper.setBlock(ceiling, Blocks.STONE);
		helper.setBlock(lamp, ModBlocks.LIGHTS.get(LightKind.CHANDELIER).get());
		LineGameTests.wire(helper, ModBlocks.COPPER_POWER_LINE.get(), w, Direction.SOUTH);
		helper.runAtTickTime(2, () -> {
			com.comedymonkey.createaddons.content.light.HangingLampBlock.lower(helper.getLevel(), helper.absolutePos(lamp));
			com.comedymonkey.createaddons.content.light.HangingLampBlock.lower(helper.getLevel(), helper.absolutePos(lamp.below()));
		});
		BlockPos shift = new BlockPos(0, 0, 8), min = new BlockPos(2, 1, 1), max = new BlockPos(2, 4, 2);
		LineGameTests.generator(helper, gen.offset(shift));
				BlockPos printed = lamp.below(2).offset(shift);
		print(helper, min, max, min.offset(shift), 8, () -> {
			var hung = com.comedymonkey.createaddons.content.light.HangingLampBlock.HUNG;
			BlockState foot = helper.getBlockState(printed);
			if (!foot.is(ModBlocks.LIGHTS.get(LightKind.CHANDELIER).get()) || !foot.getValue(hung))
				helper.fail("the printed Chandelier isn't hanging where it should: " + foot);
			for (int i = 1; i <= 2; i++)
				if (!helper.getBlockState(printed.above(i)).is(ModBlocks.HANGING_CABLE.get()))
					helper.fail("no cable " + i + " above the printed Chandelier: " + helper.getBlockState(printed.above(i)));
			if (!(helper.getLevel().getBlockEntity(helper.absolutePos(printed)) instanceof LightBlockEntity l) || l.phase() != LightBlockEntity.Phase.LIT)
				helper.fail("the printed Chandelier isn't lit: " + describe(helper, printed));
			helper.succeed();
		});
	}

	/** A printed Bollard at the end of a printed floor wire lights, as the one it was copied from does. */
	@GameTest(template = COURSE, timeoutTicks = 260)
	public static void printedBollardLights(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 2), lamp = gen.east(4);
		for (int i = 1; i <= 3; i++)
			floorWire(helper, gen.east(i));
		helper.setBlock(lamp.below(), Blocks.STONE);
		helper.setBlock(lamp, ModBlocks.LIGHTS.get(LightKind.BOLLARD).get());
		helper.runAtTickTime(2, () -> LineGameTests.relink(helper, gen.east(1), gen.east(3)));
		BlockPos shift = new BlockPos(0, 0, 8), min = gen.east(), max = lamp;
		LineGameTests.generator(helper, gen.offset(shift));
				BlockPos printed = lamp.offset(shift);
		print(helper, min, max, min.offset(shift), 8, () -> {
			if (!(helper.getLevel().getBlockEntity(helper.absolutePos(printed)) instanceof LightBlockEntity l) || l.phase() != LightBlockEntity.Phase.LIT)
				helper.fail("the printed Bollard isn't lit: " + describe(helper, printed));
			helper.succeed();
		});
	}

	/**
	 * A printed Floor Light at the end of a printed floor wire lights, as the one it was copied from does: the wire is
	 * printed first, and a Floor Light is fixed to nothing, so nothing but the light landing tells the wire it's there.
	 */
	@GameTest(template = COURSE, timeoutTicks = 260)
	public static void printedFloorLightLights(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 2), lamp = gen.east(4);
		for (int i = 1; i <= 3; i++)
			floorWire(helper, gen.east(i));
		helper.setBlock(lamp, ModBlocks.LIGHTS.get(LightKind.FLOOR_LIGHT).get());
		helper.runAtTickTime(2, () -> LineGameTests.relink(helper, gen.east(1), gen.east(3)));
		BlockPos shift = new BlockPos(0, 0, 8), min = gen.east(), max = lamp;
		LineGameTests.generator(helper, gen.offset(shift));
		BlockPos printed = lamp.offset(shift);
		print(helper, min, max, min.offset(shift), 8, () -> {
			if (!(helper.getLevel().getBlockEntity(helper.absolutePos(lamp)) instanceof LightBlockEntity copy) || copy.phase() != LightBlockEntity.Phase.DARK)
				helper.fail("the copied Floor Light should be dark (no generator)");
			if (!(helper.getLevel().getBlockEntity(helper.absolutePos(printed)) instanceof LightBlockEntity l) || l.phase() != LightBlockEntity.Phase.LIT)
				helper.fail("the printed Floor Light isn't lit: " + describe(helper, printed));
			helper.succeed();
		});
	}

	/**
	 * Lines print with their fittings, settings, wrenched joints and encasing, and the cannon spends the fitting, the casing
	 * block and lines of the right colour on them.
	 */
	@GameTest(template = COURSE, timeoutTicks = 260)
	public static void printedLinesKeepFittingsAndCasing(GameTestHelper helper) {
		BlockPos breaker = new BlockPos(2, 1, 2), cased = breaker.east(2), valve = cased.east(2), filter = valve.east(2);
		// (a red wire west of it, printed with it, for the wrench to cut it from)
		floorWire(helper, breaker.west());
		floorWire(helper, breaker);
		floorWire(helper, cased);
		helper.setBlock(valve, ModBlocks.COPPER_FLUID_LINE.get());
		helper.setBlock(filter, ModBlocks.COPPER_FLUID_LINE.get());
		helper.runAtTickTime(2, () -> {
			line(helper, breaker.west()).setDye(DyeColor.RED);
			LineBlockEntity b = line(helper, breaker);
			b.setDye(DyeColor.RED);
			b.setFitting(Fitting.BREAKER, Direction.EAST);
			b.flipLever();
			b.wrenchJoint(Direction.WEST);
			line(helper, cased).setEncasing(Blocks.STONE.defaultBlockState());
			LineBlockEntity v = line(helper, valve);
			v.setFitting(Fitting.VALVE, Direction.EAST);
			v.turnValve(-6);
			LineBlockEntity f = line(helper, filter);
			f.setFitting(Fitting.FILTER, Direction.EAST);
			f.setFilter(new FluidStack(Fluids.WATER, 1000));
		});
		BlockPos shift = new BlockPos(0, 0, 8);
		List<ItemRequirement.StackRequirement> spent = new ArrayList<>();
		print(helper, breaker.west(), filter, breaker.west().offset(shift), 5, Rotation.NONE, Mirror.NONE, spent, () -> {
			LineBlockEntity b0 = line(helper, breaker), b = line(helper, breaker.offset(shift));
			if (b0.joint(Direction.WEST) == com.comedymonkey.createaddons.content.line.Joint.AUTO)
				helper.fail("setting up: the wrench didn't change the copy's joint");
			if (b.dye() != DyeColor.RED || b.fitting() != Fitting.BREAKER || b.fittingSide() != Direction.EAST || b.leverOn()
				|| b.joint(Direction.WEST) != b0.joint(Direction.WEST))
				helper.fail("the printed Breaker wire isn't as copied: " + b.dye() + " " + b.fitting() + " " + b.fittingSide()
					+ " lever " + (b.leverOn() ? "on" : "off") + " joint " + b.joint(Direction.WEST));
			LineBlockEntity c = line(helper, cased.offset(shift));
			if (c.encasing() == null || !c.encasing().is(Blocks.STONE))
				helper.fail("the printed wire isn't encased in stone: " + c.encasing());
			LineBlockEntity v = line(helper, valve.offset(shift));
			if (v.fitting() != Fitting.VALVE || v.valveOpen() != line(helper, valve).valveOpen())
				helper.fail("the printed Valve isn't as copied: " + v.fitting() + " open " + v.valveOpen() + "/10, not " + line(helper, valve).valveOpen());
			LineBlockEntity f = line(helper, filter.offset(shift));
			if (f.fitting() != Fitting.FILTER || !f.filter().is(Fluids.WATER))
				helper.fail("the printed Filter isn't set to water: " + f.fitting() + " " + f.filter().getFluid());
			ItemStack red = new ItemStack(ModBlocks.COPPER_POWER_LINE.get());
			red.set(ModDataComponents.LINE_DYE.get(), DyeColor.RED);
			expect(helper, spent, "red Copper Power Lines", red, 2);
			expect(helper, spent, "a plain Copper Power Line", new ItemStack(ModBlocks.COPPER_POWER_LINE.get()), 1);
			expect(helper, spent, "Copper Fluid Lines", new ItemStack(ModBlocks.COPPER_FLUID_LINE.get()), 2);
			expect(helper, spent, "a Breaker", new ItemStack(Fitting.BREAKER.item()), 1);
			expect(helper, spent, "a Valve", new ItemStack(Fitting.VALVE.item()), 1);
			expect(helper, spent, "a Filter", new ItemStack(Fitting.FILTER.item()), 1);
			expect(helper, spent, "stone for the casing", new ItemStack(Blocks.STONE), 1);
			helper.succeed();
		});
	}

	/** Spans between two Line Posts print, unturned: see {@link #printedSpans}. */
	@GameTest(template = COURSE, timeoutTicks = 300)
	public static void printedSpansHang(GameTestHelper helper) {
		printedSpans(helper, Rotation.NONE, Mirror.NONE, new BlockPos(2, 0, 12), 2);
	}

	/** ...turned a quarter clockwise: the east branch is the south one now. */
	@GameTest(template = COURSE, timeoutTicks = 300)
	public static void printedSpansHangTurned(GameTestHelper helper) {
		printedSpans(helper, Rotation.CLOCKWISE_90, Mirror.NONE, new BlockPos(14, 0, 4), 3);
	}

	/** ...mirrored front to back: the east branch is the west one now. */
	@GameTest(template = COURSE, timeoutTicks = 300)
	public static void printedSpansHangMirrored(GameTestHelper helper) {
		printedSpans(helper, Rotation.NONE, Mirror.FRONT_BACK, new BlockPos(12, 0, 12), 4);
	}

	/**
	 * Two Line Posts with two spans between them (from the first's top and from its east branch, both to the second's top),
	 * a wire at each foot and a Bollard at the far end, printed turned and mirrored so: both posts hold both spans (the
	 * branch span now from insulator {@code branch}), none waits for its far post, power crosses them to the Bollard, and
	 * the spans' wire is paid for once, as much as their length takes (the copy says 99). A third span, to a post outside
	 * the schematic, isn't printed or paid for.
	 */
	private static void printedSpans(GameTestHelper helper, Rotation rotation, Mirror mirror, BlockPos to, int branch) {
		BlockPos gen = new BlockPos(1, 1, 3), a = gen.east(2), b = gen.east(7), lamp = b.east(2);
		floorWire(helper, gen.east());
		floorWire(helper, b.east());
		helper.setBlock(a.below(), Blocks.STONE);
		helper.setBlock(b.below(), Blocks.STONE);
		helper.setBlock(a, ModBlocks.LINE_POST.get().defaultBlockState().setValue(LinePostBlock.BRANCHES.get(Direction.EAST), true));
		helper.setBlock(b, ModBlocks.LINE_POST.get());
		helper.setBlock(lamp.below(), Blocks.STONE);
		helper.setBlock(lamp, ModBlocks.LIGHTS.get(LightKind.BOLLARD).get());
		BlockPos outside = a.south(3);
		helper.setBlock(outside.below(), Blocks.STONE);
		helper.setBlock(outside, ModBlocks.LINE_POST.get());
		helper.runAtTickTime(2, () -> {
			LinePostBlockEntity pa = post(helper, a), pb = post(helper, b);
			for (int from : new int[] {0, 2}) {
				Span s = new Span(from, b.subtract(a), 0, LineTier.COPPER, null, true, 99);
				pa.addSpan(s);
				pb.addSpan(s.reversed());
			}
			Span away = new Span(0, outside.subtract(a), 0, LineTier.COPPER, null, true, 3);
			pa.addSpan(away);
			post(helper, outside).addSpan(away.reversed());
			LineGameTests.relink(helper, gen.east(), b.east());
		});
		BlockPos min = new BlockPos(2, 0, 3), max = new BlockPos(10, 1, 3);
		generator(helper, landed(min, to, gen, rotation, mirror), rotation, mirror);
		List<ItemRequirement.StackRequirement> spent = new ArrayList<>();
		print(helper, min, max, to, 5, rotation, mirror, spent, () -> {
			BlockPos pa = landed(min, to, a, rotation, mirror), pb = landed(min, to, b, rotation, mirror);
			LinePostBlockEntity printedA = post(helper, pa), printedB = post(helper, pb);
			BlockPos offset = pb.subtract(pa);
			int wires = 0;
			for (int from : new int[] {0, branch}) {
				int w = wiresBetween(helper, pa, from, pb, 0);
				wires += w;
				Span s = new Span(from, offset, 0, LineTier.COPPER, null, true, w);
				if (!printedA.spans().contains(s) || !printedB.spans().contains(s.reversed()))
					helper.fail("the span from insulator " + from + " isn't held at both printed posts: " + printedA.spans() + " / " + printedB.spans());
			}
			if (printedA.spans().size() != 2 || printedB.spans().size() != 2)
				helper.fail("the printed posts hold " + printedA.spans().size() + " and " + printedB.spans().size() + " spans, not 2 each");
			if (printedA.waitingForFarPost() || printedB.waitingForFarPost())
				helper.fail("a printed span still waits for its far post, though both are printed");
			BlockPos bollard = landed(min, to, lamp, rotation, mirror);
			if (!(helper.getLevel().getBlockEntity(helper.absolutePos(bollard)) instanceof LightBlockEntity l) || l.phase() != LightBlockEntity.Phase.LIT)
				helper.fail("the printed Bollard isn't lit across the printed spans: " + describe(helper, bollard));
			// two posts and a branch; two foot wires and two spans of two wires each, paid once
			expect(helper, spent, "Line Posts", new ItemStack(ModBlocks.LINE_POST.get()), 3);
			expect(helper, spent, "Copper Power Lines", new ItemStack(ModBlocks.COPPER_POWER_LINE.get()), 2 + wires);
			if (!helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
				new net.minecraft.world.phys.AABB(helper.absolutePos(to)).inflate(12)).isEmpty())
				helper.fail("something dropped as items while printing (a span coming down?)");
			helper.succeed();
		});
	}

	// ---------- settings (user, 2026-11-05: every setting prints; contents don't) ----------

	/**
	 * Lights print turned a quarter: a Bollard's and an Edge Light's brightness, the Edge Light's edge (turned), and where a
	 * Floodlight is aimed (turned with the print).
	 */
	@GameTest(template = COURSE, timeoutTicks = 300)
	public static void printedLightsKeepTheirSettings(GameTestHelper helper) {
		BlockPos bollard = new BlockPos(2, 1, 2), edge = bollard.east(2), flood = bollard.south(2), target = flood.east(4);
		for (BlockPos p : new BlockPos[] {bollard, edge, flood, target})
			helper.setBlock(p.below(), Blocks.STONE);
		helper.setBlock(target, Blocks.STONE);
		helper.setBlock(bollard, ModBlocks.LIGHTS.get(LightKind.BOLLARD).get());
		helper.setBlock(flood, ModBlocks.LIGHTS.get(LightKind.FLOODLIGHT).get().defaultBlockState().setValue(LightBlock.FACING, Direction.UP));
		helper.setBlock(edge, ModBlocks.EDGE_LIGHT.get());
		// (its edge set as it's placed: an Edge Light with no edges goes)
		int edgeBit = com.comedymonkey.createaddons.content.light.EdgeLinks.bit(Direction.DOWN, Direction.NORTH);
		call(light(helper, edge), "setEdges", int.class, edgeBit);
		helper.runAtTickTime(2, () -> {
			light(helper, bollard).stepTo(2, false);
			light(helper, edge).stepTo(1, false);
			var aimed = com.comedymonkey.createaddons.content.light.Floodlights.aim(helper.getLevel(), light(helper, flood), helper.absolutePos(target));
			if (aimed != com.comedymonkey.createaddons.content.light.Floodlights.Result.AIMED)
				helper.fail("setting up: the Floodlight didn't aim: " + aimed);
		});
		Rotation turn = Rotation.CLOCKWISE_90;
		BlockPos min = new BlockPos(2, 0, 2), max = target, to = new BlockPos(14, 0, 4);
		print(helper, min, max, to, 5, turn, Mirror.NONE, new ArrayList<>(), () -> {
			if (light(helper, landed(min, to, bollard, turn, Mirror.NONE)).step() != 2)
				helper.fail("the printed Bollard's brightness isn't step 2");
			var e = (com.comedymonkey.createaddons.content.light.EdgeLightBlockEntity) light(helper, landed(min, to, edge, turn, Mirror.NONE));
			if (e.step() != 1 || !com.comedymonkey.createaddons.content.light.EdgeLinks.has(e.edges(), Direction.DOWN, turn.rotate(Direction.NORTH)))
				helper.fail("the printed Edge Light isn't at step 1 along its turned edge: step " + e.step() + ", edges " + e.edges());
			LightBlockEntity f = light(helper, landed(min, to, flood, turn, Mirror.NONE)), f0 = light(helper, flood);
			if (!helper.absolutePos(landed(min, to, target, turn, Mirror.NONE)).equals(f.aim()) || f.aimFace() != turn.rotate(f0.aimFace()))
				helper.fail("the printed Floodlight isn't aimed at the printed target: " + f.aim() + " " + f.aimFace() + ", not "
					+ helper.absolutePos(landed(min, to, target, turn, Mirror.NONE)) + " " + turn.rotate(f0.aimFace()));
			helper.succeed();
		});
	}

	/** A Festoon prints turned a quarter: both hooks hold their string (turned), none waits, and its Festoon Lights are spent. */
	@GameTest(template = COURSE, timeoutTicks = 300)
	public static void printedFestoonHangs(GameTestHelper helper) {
		BlockPos west = new BlockPos(2, 3, 3), a = west.east(), b = a.east(5), east = b.east();
		helper.setBlock(west, Blocks.STONE);
		helper.setBlock(east, Blocks.STONE);
		var hung = com.comedymonkey.createaddons.content.light.Festoons.hang(helper.getLevel(), helper.absolutePos(a), Direction.EAST,
			helper.absolutePos(b), Direction.WEST, new ItemStack(com.comedymonkey.createaddons.registry.ModItems.FESTOON_LIGHTS.get(), 8), null);
		if (hung != com.comedymonkey.createaddons.content.light.Festoons.Result.HUNG)
			helper.fail("setting up: the festoon didn't hang: " + hung);
		Rotation turn = Rotation.CLOCKWISE_90;
		BlockPos to = new BlockPos(14, 3, 4);
		List<ItemRequirement.StackRequirement> spent = new ArrayList<>();
		print(helper, west, east, to, 3, turn, Mirror.NONE, spent, () -> {
			var pa = festoon(helper, landed(west, to, a, turn, Mirror.NONE));
			var pb = festoon(helper, landed(west, to, b, turn, Mirror.NONE));
			if (!pa.partners().equals(List.of(pb.getBlockPos())) || !pb.partners().equals(List.of(pa.getBlockPos())))
				helper.fail("the printed hooks don't hold their string to each other: " + pa.partners() + " / " + pb.partners());
			Direction bFaces = helper.getBlockState(landed(west, to, b, turn, Mirror.NONE)).getValue(com.comedymonkey.createaddons.content.light.FestoonHookBlock.FACING);
			if (pa.spans().get(0).facing() != bFaces)
				helper.fail("the printed string says its far hook faces " + pa.spans().get(0).facing() + ", not " + bFaces);
			if (pa.waitingForFarHook() || pb.waitingForFarHook())
				helper.fail("a printed string still waits for its far hook, though both are printed");
			int cost = festoon(helper, a).heldCost() + festoon(helper, b).heldCost();
			expect(helper, spent, "Festoon Lights", new ItemStack(com.comedymonkey.createaddons.registry.ModItems.FESTOON_LIGHTS.get()), cost);
			helper.succeed();
		});
	}

	/**
	 * Machines print with their settings and not their contents: an Electric Motor's speed, stress and direction, a
	 * Dynamo's stress, a Geothermal Controller's output, a tube's shut joint, chest and barrel names (and the cannon
	 * spends a chest and barrel of exactly their tier).
	 */
	@GameTest(template = COURSE, timeoutTicks = 300)
	public static void printedMachinesKeepTheirSettings(GameTestHelper helper) {
		BlockPos motor = new BlockPos(2, 1, 2), dynamo = motor.east(2), geo = dynamo.east(2), tube = geo.east(2), chest = tube.east(2), barrel = chest.east(2);
		helper.setBlock(motor, ModBlocks.ELECTRIC_MOTOR.get());
		helper.setBlock(dynamo, ModBlocks.CLOCKWORK_DYNAMO.get());
		helper.setBlock(geo, ModBlocks.GEOTHERMAL_CONTROLLER.get());
		helper.setBlock(tube, ModBlocks.CLOCKWORK_TUBE.get());
		helper.setBlock(chest, ModBlocks.UPGRADED_CHEST.get().defaultBlockState().setValue(com.comedymonkey.createaddons.content.chest.UpgradedChestBlock.TIER,
			com.comedymonkey.createaddons.content.chest.ChestTier.DIAMOND));
		helper.setBlock(barrel, ModBlocks.UPGRADED_BARREL.get().defaultBlockState().setValue(com.comedymonkey.createaddons.content.chest.UpgradedBarrelBlock.TIER,
			com.comedymonkey.createaddons.content.chest.ChestTier.GOLD));
		helper.runAtTickTime(2, () -> {
			((com.comedymonkey.createaddons.content.electric.ElectricMotorBlockEntity) be(helper, motor)).applySettings(96,
				2 * com.comedymonkey.createaddons.content.electric.ElectricMotorBlockEntity.STRESS_STEP, true);
			((com.comedymonkey.createaddons.content.electric.ClockworkDynamoBlockEntity) be(helper, dynamo)).applySettings(
				3 * com.comedymonkey.createaddons.content.electric.ElectricMotorBlockEntity.STRESS_STEP);
			((com.comedymonkey.createaddons.content.electric.GeothermalControllerBlockEntity) be(helper, geo)).applyOutput(4);
			call(be(helper, tube), "setShut", new Class<?>[] {Direction.class, boolean.class}, Direction.UP, true);
			named(be(helper, chest), "Ore Store");
			named(be(helper, barrel), "Seeds");
		});
		BlockPos shift = new BlockPos(0, 0, 8);
		List<ItemRequirement.StackRequirement> spent = new ArrayList<>();
		print(helper, motor, barrel, motor.offset(shift), 5, Rotation.NONE, Mirror.NONE, spent, () -> {
			var m = (com.comedymonkey.createaddons.content.electric.ElectricMotorBlockEntity) be(helper, motor.offset(shift));
			if (m.getSpeedSetting() != 96 || m.getStressSetting() != 2 * com.comedymonkey.createaddons.content.electric.ElectricMotorBlockEntity.STRESS_STEP || !m.isReversed())
				helper.fail("the printed Electric Motor isn't as set: " + m.getSpeedSetting() + " rpm, " + m.getStressSetting() + " su, reversed " + m.isReversed());
			var d = (com.comedymonkey.createaddons.content.electric.ClockworkDynamoBlockEntity) be(helper, dynamo.offset(shift));
			if (d.getStressSetting() != 3 * com.comedymonkey.createaddons.content.electric.ElectricMotorBlockEntity.STRESS_STEP)
				helper.fail("the printed Dynamo's stress is " + d.getStressSetting());
			var g = (com.comedymonkey.createaddons.content.electric.GeothermalControllerBlockEntity) be(helper, geo.offset(shift));
			if (g.outputTenths() != 4)
				helper.fail("the printed Geothermal Controller's output is " + g.outputTenths() + "/10, not 4/10");
			var t = (com.comedymonkey.createaddons.content.tube.ClockworkTubeBlockEntity) be(helper, tube.offset(shift));
			if (!t.isShut(Direction.UP))
				helper.fail("the printed tube's top joint isn't shut");
			for (BlockPos[] p : new BlockPos[][] {{chest, null}, {barrel, null}}) {
				var c = (net.minecraft.world.level.block.entity.BaseContainerBlockEntity) be(helper, p[0].offset(shift));
				String want = p[0] == chest ? "Ore Store" : "Seeds";
				if (c.getCustomName() == null || !want.equals(c.getCustomName().getString()))
					helper.fail("the printed " + (p[0] == chest ? "chest" : "barrel") + " isn't named " + want + ": " + c.getCustomName());
			}
			// what breaking them gives back: a plain chest and barrel, and the upgrades for their tiers
			expect(helper, spent, "a chest", new ItemStack(net.minecraft.world.item.Items.CHEST), 1);
			expect(helper, spent, "a Diamond Chest Upgrade", new ItemStack(com.comedymonkey.createaddons.content.chest.ChestTier.DIAMOND.upgradeItem()), 1);
			expect(helper, spent, "a barrel", new ItemStack(net.minecraft.world.item.Items.BARREL), 1);
			expect(helper, spent, "a Gold Upgrade", new ItemStack(com.comedymonkey.createaddons.content.chest.ChestTier.GOLD.upgradeItem()), 1);
			ItemStack copper = new ItemStack(com.comedymonkey.createaddons.content.chest.ChestTier.COPPER.upgradeItem());
			if (spent.stream().anyMatch(r -> r.matches(copper)))
				helper.fail("a Copper Upgrade would pay for the Diamond chest");
			helper.succeed();
		});
	}

	/** A dyed, named Helper Bot prints dyed and named; a Helper Bot of his colour pays for him (any name), a plain one doesn't. */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void printedHelperBotKeepsHisColour(GameTestHelper helper) {
		BlockPos bot = new BlockPos(3, 1, 3);
		helper.setBlock(bot.below(), Blocks.STONE);
		helper.setBlock(bot, ModBlocks.HELPER_BOT.get());
		helper.runAtTickTime(2, () -> be(helper, bot).applyComponents(net.minecraft.core.component.DataComponentMap.builder()
			.set(ModDataComponents.HELPER_BOT_DYE.get(), DyeColor.RED)
			.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Gizmo")).build(),
			net.minecraft.core.component.DataComponentPatch.EMPTY));
		BlockPos shift = new BlockPos(0, 0, 8);
		List<ItemRequirement.StackRequirement> spent = new ArrayList<>();
		print(helper, bot.below(), bot, bot.below().offset(shift), 5, Rotation.NONE, Mirror.NONE, spent, () -> {
			var b = (com.comedymonkey.createaddons.content.helperbot.HelperBotBlockEntity) be(helper, bot.offset(shift));
			if (b.dye() != DyeColor.RED || b.name() == null || !"Gizmo".equals(b.name().getString()))
				helper.fail("the printed Helper Bot is " + b.dye() + " named " + b.name() + ", not red Gizmo");
			ItemStack red = new ItemStack(ModBlocks.HELPER_BOT.get());
			red.set(ModDataComponents.HELPER_BOT_DYE.get(), DyeColor.RED);
			red.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Somebody else"));
			var botCost = spent.stream().filter(r -> r.stack.is(ModBlocks.HELPER_BOT.get().asItem())).toList();
			if (botCost.size() != 1 || !botCost.get(0).matches(red) || botCost.get(0).matches(new ItemStack(ModBlocks.HELPER_BOT.get())))
				helper.fail("the cannon doesn't want exactly a red Helper Bot (any name) for him: " + botCost.stream().map(r -> r.stack).toList());
			helper.succeed();
		});
	}

	/**
	 * A Wellspring prints with its upgrades, each paid for (equal ones as one charge), and nothing else that was in its
	 * slots (a diamond slipped into one), and not its fluid.
	 */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void printedWellspringKeepsItsUpgrades(GameTestHelper helper) {
		BlockPos well = new BlockPos(3, 1, 3);
		helper.setBlock(well, ModBlocks.WELLSPRING.get());
		helper.runAtTickTime(2, () -> {
			var w = (com.comedymonkey.createaddons.content.wellspring.WellspringBlockEntity) be(helper, well);
			w.getUpgrades().setStackInSlot(0, new ItemStack(com.comedymonkey.createaddons.registry.ModItems.WATER_UPGRADE.get()));
			w.getUpgrades().setStackInSlot(1, new ItemStack(com.comedymonkey.createaddons.registry.ModItems.WATER_UPGRADE.get()));
			w.getUpgrades().setStackInSlot(2, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 7));
			for (int i = 0; i < 3; i++)
				w.getUpgrades().setStackInSlot(com.comedymonkey.createaddons.content.wellspring.WellspringUpgradeInventory.FIRST_CAPACITY_SLOT + i,
					new ItemStack(com.comedymonkey.createaddons.registry.ModItems.CAPACITY_UPGRADE.get()));
		});
		BlockPos shift = new BlockPos(0, 0, 8);
		List<ItemRequirement.StackRequirement> spent = new ArrayList<>();
		print(helper, well, well, well.offset(shift), 5, Rotation.NONE, Mirror.NONE, spent, () -> {
			var w = (com.comedymonkey.createaddons.content.wellspring.WellspringBlockEntity) be(helper, well.offset(shift));
			if (!w.getUpgrades().getStackInSlot(1).is(com.comedymonkey.createaddons.registry.ModItems.WATER_UPGRADE.get())
				|| !w.getUpgrades().getStackInSlot(2).isEmpty() || w.getCapacityUpgradeCount() != 3)
				helper.fail("the printed Wellspring's slots aren't its upgrades alone: " + w.getUpgrades().getStackInSlot(1) + ", "
					+ w.getUpgrades().getStackInSlot(2) + ", " + w.getCapacityUpgradeCount() + " capacity");
			expect(helper, spent, "a Wellspring", new ItemStack(ModBlocks.WELLSPRING.get()), 1);
			expect(helper, spent, "Water Upgrades", new ItemStack(com.comedymonkey.createaddons.registry.ModItems.WATER_UPGRADE.get()), 2);
			expect(helper, spent, "Capacity Upgrades", new ItemStack(com.comedymonkey.createaddons.registry.ModItems.CAPACITY_UPGRADE.get()), 3);
			if (spent.stream().anyMatch(r -> r.stack.is(net.minecraft.world.item.Items.DIAMOND)))
				helper.fail("the cannon charges for the diamond in the copy's slot (and would print it)");
			helper.succeed();
		});
	}

	/** An Industrial Battery tower with a span to a post prints unturned: see {@link #printedBattery}. */
	@GameTest(template = COURSE, timeoutTicks = 400)
	public static void printedBatteryTowerKeepsItsSpan(GameTestHelper helper) {
		printedBattery(helper, Rotation.NONE, Mirror.NONE, new BlockPos(2, 0, 10));
	}

	/** ...turned a quarter clockwise. */
	@GameTest(template = COURSE, timeoutTicks = 400)
	public static void printedBatteryTowerKeepsItsSpanTurned(GameTestHelper helper) {
		printedBattery(helper, Rotation.CLOCKWISE_90, Mirror.NONE, new BlockPos(20, 0, 8));
	}

	/** ...mirrored front to back: its front-right insulator is its front-left one now. */
	@GameTest(template = COURSE, timeoutTicks = 400)
	public static void printedBatteryTowerKeepsItsSpanMirrored(GameTestHelper helper) {
		printedBattery(helper, Rotation.NONE, Mirror.FRONT_BACK, new BlockPos(20, 0, 14));
	}

	/**
	 * A schematic saying the battery's end draws the span (a player can't hang one so; the post's end pays, so nobody
	 * would have): it isn't printed at either end, and no wire is spent on it.
	 */
	@GameTest(template = COURSE, timeoutTicks = 400)
	public static void printedBatterySpanTheBatteryClaimsDoesntPrint(GameTestHelper helper) {
		printedBattery(helper, Rotation.NONE, Mirror.NONE, new BlockPos(2, 0, 10), true);
	}

	/**
	 * A 3x3x3 Industrial Battery facing east (the first Battery placed faced east: the rest face north, so a tower that took
	 * the front of the block the cannon finished it with would face the wrong way), with a span from its front-right
	 * insulator to a Line Post: printed turned and mirrored so, it forms facing the turned front, its master and the post
	 * both hold the span (from the mirrored insulator), neither waits, and the post's end pays the wire.
	 */
	private static void printedBattery(GameTestHelper helper, Rotation rotation, Mirror mirror, BlockPos to) {
		printedBattery(helper, rotation, mirror, to, false);
	}

	private static void printedBattery(GameTestHelper helper, Rotation rotation, Mirror mirror, BlockPos to, boolean batteryOwns) {
		BlockPos c = new BlockPos(2, 1, 2), post = new BlockPos(8, 1, 3);
		for (int y = 0; y < 3; y++)
			for (int z = 0; z < 3; z++)
				for (int x = 0; x < 3; x++)
					helper.setBlock(c.offset(x, y, z), ModBlocks.BATTERY.get().defaultBlockState().setValue(
						com.comedymonkey.createaddons.content.electric.BatteryBlock.FACING, x + y + z == 0 ? Direction.EAST : Direction.NORTH));
		helper.setBlock(post.below(), Blocks.STONE);
		helper.setBlock(post, ModBlocks.LINE_POST.get());
		// facing east: the master is the middle of the east row of the bottom layer
		BlockPos master = c.offset(2, 0, 1);
		helper.runAtTickTime(4, () -> {
			var m = battery(helper, master);
			if (!m.isMaster() || m.towerFacing() != Direction.EAST)
				helper.fail("setting up: the tower didn't form facing east on " + master + " (" + m.towerFacing() + ")");
			Span s = new Span(1, post.subtract(master), 0, LineTier.COPPER, null, batteryOwns, 99);
			m.addSpan(s);
			post(helper, post).addSpan(s.reversed());
		});
		BlockPos min = new BlockPos(2, 0, 2), max = new BlockPos(8, 3, 4);
		List<ItemRequirement.StackRequirement> spent = new ArrayList<>();
		print(helper, min, max, to, 6, rotation, mirror, spent, () -> {
			BlockPos pm = landed(min, to, master, rotation, mirror), pp = landed(min, to, post, rotation, mirror);
			var m = battery(helper, pm);
			Direction front = rotation.rotate(mirror.mirror(Direction.EAST));
			if (!m.isMaster() || m.towerFacing() != front)
				helper.fail("the printed tower isn't formed facing " + front + " on " + pm + ": master " + m.isMaster() + ", facing " + m.towerFacing());
			int insulator = mirror == Mirror.NONE ? 1 : 0;
			LinePostBlockEntity p = post(helper, pp);
			expect(helper, spent, "Batteries", new ItemStack(ModBlocks.BATTERY.get()), 27);
			if (batteryOwns) {
				if (!m.spans().isEmpty() || !p.spans().isEmpty())
					helper.fail("a span the battery claims was printed: tower " + m.spans() + ", post " + p.spans());
				expect(helper, spent, "Copper Power Lines", new ItemStack(ModBlocks.COPPER_POWER_LINE.get()), 0);
				helper.succeed();
				return;
			}
			int wires = LinePostBlockEntity.wireCount(m.tie(insulator).distanceTo(
				LinePostBlock.tie(helper.getBlockState(pp), helper.absolutePos(pp), 0)));
			Span s = new Span(insulator, pp.subtract(pm), 0, LineTier.COPPER, null, false, wires);
			if (!m.spans().equals(List.of(s)) || !p.spans().equals(List.of(s.reversed())))
				helper.fail("the printed span isn't " + s + " at both ends: tower " + m.spans() + ", post " + p.spans());
			if (m.waitingForPost() || p.waitingForFarPost())
				helper.fail("the printed span still waits, though both ends are printed");
			expect(helper, spent, "Copper Power Lines (the post's end of the span)", new ItemStack(ModBlocks.COPPER_POWER_LINE.get()), wires);
			helper.succeed();
		});
	}

	/**
	 * A Trade Bot's station prints turned a quarter with its links (turned, a chest outside the schematic too, as a link
	 * is to wherever it points; one a schematic says is out of his range is dropped) and paused, and a new bot docks in it who is nobody's: he doesn't work, no one controls him, anyone may wind
	 * him up and pack him up. A fresh Trade Bot pays for him; one carrying emeralds doesn't.
	 */
	@GameTest(template = COURSE, timeoutTicks = 300)
	public static void printedTradeBotStation(GameTestHelper helper) {
		BlockPos station = new BlockPos(3, 1, 3), chest = station.west(), barrel = station.east();
		for (BlockPos p : new BlockPos[] {chest, station, barrel})
			helper.setBlock(p.below(), Blocks.STONE);
		helper.setBlock(chest, Blocks.CHEST);
		helper.setBlock(barrel, Blocks.BARREL);
		BlockPos away = station.south();
		helper.setBlock(away, Blocks.CHEST);
		helper.setBlock(station, ModBlocks.TRADE_BOT.get().defaultBlockState().setValue(com.comedymonkey.createaddons.content.tradebot.TradeBotStationBlock.FACING, Direction.NORTH));
		var data = new com.comedymonkey.createaddons.content.tradebot.TradeBotData(0, 0, 0, java.util.Map.of(),
			net.minecraft.world.item.component.ItemContainerContents.EMPTY, List.of(chest.subtract(station), away.subtract(station), new BlockPos(0, 0, 300)),
			java.util.Optional.of(barrel.subtract(station)));
		if (com.comedymonkey.createaddons.content.tradebot.TradeBotItem.populate(helper.getLevel(), helper.absolutePos(station), java.util.UUID.randomUUID(), data, null) == null)
			helper.fail("setting up: no bot in the station");
		helper.runAtTickTime(2, () -> call(be(helper, station), "setPaused", boolean.class, true));
		Rotation turn = Rotation.CLOCKWISE_90;
		BlockPos min = chest.below(), max = barrel, to = new BlockPos(14, 0, 6);
		List<ItemRequirement.StackRequirement> spent = new ArrayList<>();
		print(helper, min, max, to, 5, turn, Mirror.NONE, spent, () -> {
			var s = (com.comedymonkey.createaddons.content.tradebot.TradeBotStationBlockEntity) be(helper, landed(min, to, station, turn, Mirror.NONE));
			if (!com.comedymonkey.createaddons.content.tradebot.TradeBotOwnership.NOBODY.equals(s.getOwner()))
				helper.fail("the printed station's owner is " + s.getOwner() + ", not nobody");
			var bot = s.findBot();
			if (bot == null || !bot.isDocked())
				helper.fail("no bot docked in the printed station");
			if (!s.inputPositions().equals(List.of(helper.absolutePos(landed(min, to, chest, turn, Mirror.NONE)), helper.absolutePos(landed(min, to, away, turn, Mirror.NONE))))
				|| !helper.absolutePos(landed(min, to, barrel, turn, Mirror.NONE)).equals(s.outputPosition()) || !s.isPaused())
				helper.fail("the printed station's links or pause aren't as copied: takes " + s.inputPositions() + ", delivers " + s.outputPosition() + ", paused " + s.isPaused());
			var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
			if (com.comedymonkey.createaddons.content.tradebot.TradeBotOwnership.canControl(player, s.getOwner())
				|| !com.comedymonkey.createaddons.content.tradebot.TradeBotOwnership.canPackUp(player, s.getOwner()))
				helper.fail("a player can control the printed bot, or can't pack him up");
			if (!s.waitsForOwner())
				helper.fail("the printed bot would work, nobody's");
			bot.windDown(com.comedymonkey.createaddons.content.tradebot.TradeBotEntity.Rundown.DAMAGE);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", "wrench"))));
			bot.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
			if (bot.rundown() != null)
				helper.fail("a player's wrench didn't wind up the printed bot (nobody else could)");
			ItemStack fresh = new ItemStack(com.comedymonkey.createaddons.registry.ModItems.TRADE_BOT.get()), rich = fresh.copy();
			rich.set(ModDataComponents.TRADE_BOT_DATA.get(), new com.comedymonkey.createaddons.content.tradebot.TradeBotData(0, 5, 0, java.util.Map.of(),
				net.minecraft.world.item.component.ItemContainerContents.EMPTY, List.of(), java.util.Optional.empty()));
			var botCost = spent.stream().filter(r -> r.stack.is(fresh.getItem())).toList();
			if (botCost.size() != 1 || !botCost.get(0).matches(fresh) || botCost.get(0).matches(rich))
				helper.fail("the cannon doesn't want exactly a fresh Trade Bot for the station: " + botCost.stream().map(r -> r.stack).toList());
			helper.succeed();
		});
	}

	// ---------- what the review found (2026-11-05) ----------

	/** A Splitter prints on its junction (its branches printed after it) and keeps its priorities; it doesn't pop off. */
	@GameTest(template = COURSE, timeoutTicks = 260)
	public static void printedSplitterStays(GameTestHelper helper) {
		BlockPos t = new BlockPos(3, 1, 3);
		for (BlockPos p : new BlockPos[] {t.west(), t, t.east(), t.south()})
			floorWire(helper, p);
		byte[] levels = {1, 1, 1, 1, 1, 1};
		levels[Direction.EAST.ordinal()] = 0;
		levels[Direction.SOUTH.ordinal()] = 2;
		helper.runAtTickTime(2, () -> {
			LineGameTests.relink(helper, t.west(), t, t.east(), t.south());
			var s = (com.comedymonkey.createaddons.content.line.PowerLineBlockEntity) line(helper, t);
			s.setFitting(Fitting.SPLITTER, Direction.EAST);
			s.setPriorities(levels.clone());
		});
		BlockPos shift = new BlockPos(0, 0, 8), min = t.west().below(), max = t.east().south();
		List<ItemRequirement.StackRequirement> spent = new ArrayList<>();
		print(helper, min, max, min.offset(shift), 5, Rotation.NONE, Mirror.NONE, spent, () -> {
			var s = (com.comedymonkey.createaddons.content.line.PowerLineBlockEntity) line(helper, t.offset(shift));
			if (s.fitting() != Fitting.SPLITTER || !java.util.Arrays.equals(s.priorities(), levels))
				helper.fail("the printed Splitter isn't on its junction with its priorities: " + s.fitting() + " " + java.util.Arrays.toString(s.priorities()));
			if (!helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
				new net.minecraft.world.phys.AABB(helper.absolutePos(t.offset(shift))).inflate(3)).isEmpty())
				helper.fail("something dropped while the Splitter was printed (it came off?)");
			expect(helper, spent, "a Splitter", new ItemStack(Fitting.SPLITTER.item()), 1);
			helper.succeed();
		});
	}

	/**
	 * Hanging posts (under a ceiling) print turned a quarter: the span from the first's east crossarm stays on a crossarm
	 * the printed post has, held at both ends.
	 */
	@GameTest(template = COURSE, timeoutTicks = 260)
	public static void printedHangingPostsKeepTheirSpan(GameTestHelper helper) {
		BlockPos a = new BlockPos(3, 3, 3), b = a.east(5);
		for (BlockPos p : new BlockPos[] {a, b})
			helper.setBlock(p.above(), Blocks.STONE);
		helper.setBlock(a, ModBlocks.LINE_POST.get().defaultBlockState().setValue(LinePostBlock.FACING, Direction.DOWN)
			.setValue(LinePostBlock.BRANCHES.get(Direction.EAST), true));
		helper.setBlock(b, ModBlocks.LINE_POST.get().defaultBlockState().setValue(LinePostBlock.FACING, Direction.DOWN));
		helper.runAtTickTime(3, () -> {
			int w = wiresBetween(helper, a, 2, b, 0);
			Span s = new Span(2, b.subtract(a), 0, LineTier.COPPER, null, true, w);
			post(helper, a).addSpan(s);
			post(helper, b).addSpan(s.reversed());
		});
		Rotation turn = Rotation.CLOCKWISE_90;
		BlockPos min = a, max = b.above(), to = new BlockPos(14, 3, 4);
		print(helper, min, max, to, 6, turn, Mirror.NONE, new ArrayList<>(), () -> {
			BlockPos pa = landed(min, to, a, turn, Mirror.NONE), pb = landed(min, to, b, turn, Mirror.NONE);
			LinePostBlockEntity printedA = post(helper, pa), printedB = post(helper, pb);
			if (printedA.spans().size() != 1 || printedB.spans().size() != 1 || !printedB.spans().get(0).equals(printedA.spans().get(0).reversed()))
				helper.fail("the printed span isn't held at both hanging posts: " + printedA.spans() + " / " + printedB.spans());
			int from = printedA.spans().get(0).from();
			if (!LinePostBlock.hasInsulator(helper.getBlockState(pa), from) || from == 0)
				helper.fail("the printed span hangs from insulator " + from + ", which isn't a crossarm the printed post has: " + helper.getBlockState(pa));
			helper.succeed();
		});
	}

	/**
	 * Printed over a post that's already there (a cannon replacing blocks), the post there gives back the wire of the span
	 * it held, and its far post lets the span go.
	 */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void printingOverAPostGivesItsSpansBack(GameTestHelper helper) {
		BlockPos copy = new BlockPos(3, 1, 3), there = copy.south(8), far = there.east(4);
		helper.setBlock(copy.below(), Blocks.STONE);
		helper.setBlock(copy, ModBlocks.LINE_POST.get().defaultBlockState().setValue(LinePostBlock.BRANCHES.get(Direction.EAST), true));
		for (BlockPos p : new BlockPos[] {there, far}) {
			helper.setBlock(p.below(), Blocks.STONE);
			helper.setBlock(p, ModBlocks.LINE_POST.get());
		}
		helper.runAtTickTime(2, () -> {
			Span s = new Span(0, far.subtract(there), 0, LineTier.COPPER, null, true, 3);
			post(helper, there).addSpan(s);
			post(helper, far).addSpan(s.reversed());
		});
		print(helper, copy.below(), copy, there.below(), 5, Rotation.NONE, Mirror.NONE, new ArrayList<>(), () -> {
			if (!post(helper, there).spans().isEmpty() || !post(helper, far).spans().isEmpty())
				helper.fail("the span is still up after the post was printed over: " + post(helper, there).spans() + " / " + post(helper, far).spans());
			int wires = 0;
			for (var e : helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
				new net.minecraft.world.phys.AABB(helper.absolutePos(there)).inflate(3)))
				if (e.getItem().is(ModBlocks.COPPER_POWER_LINE.get().asItem()))
					wires += e.getItem().getCount();
			if (wires != 3)
				helper.fail("the post printed over gave back " + wires + " Power Lines, not the 3 its span took");
			helper.succeed();
		});
	}

	/** Breaking the far hook of a printed Festoon gives back the Festoon Lights it was printed with, once. */
	@GameTest(template = COURSE, timeoutTicks = 300)
	public static void printedFestoonGivesItsLightsBack(GameTestHelper helper) {
		BlockPos west = new BlockPos(2, 3, 3), a = west.east(), b = a.east(5), east = b.east();
		helper.setBlock(west, Blocks.STONE);
		helper.setBlock(east, Blocks.STONE);
		var hung = com.comedymonkey.createaddons.content.light.Festoons.hang(helper.getLevel(), helper.absolutePos(a), Direction.EAST,
			helper.absolutePos(b), Direction.WEST, new ItemStack(com.comedymonkey.createaddons.registry.ModItems.FESTOON_LIGHTS.get(), 8), null);
		if (hung != com.comedymonkey.createaddons.content.light.Festoons.Result.HUNG)
			helper.fail("setting up: the festoon didn't hang: " + hung);
		BlockPos shift = new BlockPos(0, 0, 8);
		List<ItemRequirement.StackRequirement> spent = new ArrayList<>();
		print(helper, west, east, west.offset(shift), 3, Rotation.NONE, Mirror.NONE, spent, () -> {
			// (the far hook is the one that doesn't hold the string)
			BlockPos farHook = festoon(helper, a.offset(shift)).spans().get(0).held() ? b.offset(shift) : a.offset(shift);
			helper.getLevel().destroyBlock(helper.absolutePos(farHook), false);
			int lights = 0;
			for (var e : helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
				new net.minecraft.world.phys.AABB(helper.absolutePos(a.offset(shift))).inflate(8)))
				if (e.getItem().is(com.comedymonkey.createaddons.registry.ModItems.FESTOON_LIGHTS.get()))
					lights += e.getItem().getCount();
			int paid = spent.stream().filter(r -> r.stack.is(com.comedymonkey.createaddons.registry.ModItems.FESTOON_LIGHTS.get())).mapToInt(r -> r.stack.getCount()).sum();
			if (paid <= 0 || lights != paid)
				helper.fail("breaking the printed Festoon's far hook gave back " + lights + " Festoon Lights, not the " + paid + " it was printed with");
			helper.succeed();
		});
	}

	/** A copy whose hook holds a string to itself (a player can't hang one): the string isn't printed, and costs nothing. */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void printedFestoonToItselfDoesntPrint(GameTestHelper helper) {
		BlockPos wall = new BlockPos(2, 3, 3), hook = wall.east();
		helper.setBlock(wall, Blocks.STONE);
		helper.setBlock(hook, ModBlocks.FESTOON_HOOK.get().defaultBlockState().setValue(com.comedymonkey.createaddons.content.light.FestoonHookBlock.FACING, Direction.EAST));
		net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag(), self = new net.minecraft.nbt.CompoundTag();
		self.putLong("Off", 0);
		self.putInt("Facing", Direction.EAST.get3DDataValue());
		self.putBoolean("Held", true);
		self.putBoolean("Paid", true);
		net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
		list.add(self);
		tag.put("Spans", list);
		festoon(helper, hook).loadWithComponents(tag, helper.getLevel().registryAccess());
		BlockPos shift = new BlockPos(0, 0, 8);
		List<ItemRequirement.StackRequirement> spent = new ArrayList<>();
		print(helper, wall, hook, wall.offset(shift), 3, Rotation.NONE, Mirror.NONE, spent, () -> {
			if (helper.getLevel().getBlockEntity(helper.absolutePos(hook.offset(shift))) instanceof com.comedymonkey.createaddons.content.light.FestoonBlockEntity f
				&& !f.spans().isEmpty())
				helper.fail("the string to itself was printed: " + f.spans());
			if (spent.stream().anyMatch(r -> r.stack.is(com.comedymonkey.createaddons.registry.ModItems.FESTOON_LIGHTS.get())))
				helper.fail("the cannon charged for the string to itself");
			helper.succeed();
		});
	}

	/**
	 * A hung lamp whose cable went keeps its pending check through a save (the check's tick is saved with it, so an unload
	 * in between mustn't leave it hanging from nothing).
	 */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void hungLampCheckIsSaved(GameTestHelper helper) {
		BlockPos lamp = new BlockPos(3, 2, 3);
		helper.setBlock(lamp.above(), Blocks.STONE);
		helper.setBlock(lamp, ModBlocks.LIGHTS.get(LightKind.CHANDELIER).get());
		LightBlockEntity before = light(helper, lamp);
		call(before, "checkHanging", new Class<?>[0]);
		var tag = before.saveWithFullMetadata(helper.getLevel().registryAccess());
		var after = (LightBlockEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(helper.absolutePos(lamp), helper.getBlockState(lamp), tag,
			helper.getLevel().registryAccess());
		if (after == null || !Boolean.TRUE.equals(callFor(after, "takeHangingCheck")))
			helper.fail("the hung lamp's pending check was lost in saving");
		helper.succeed();
	}

	/** The wire a span between insulators of the two posts takes for its length (as it's hung). */
	private static int wiresBetween(GameTestHelper helper, BlockPos a, int from, BlockPos b, int to) {
		return LinePostBlockEntity.wireCount(LinePostBlock.tie(helper.getBlockState(a), helper.absolutePos(a), from)
			.distanceTo(LinePostBlock.tie(helper.getBlockState(b), helper.absolutePos(b), to)));
	}

	/** Calls a method the game keeps to its own package and returns what it gives (test setup only). */
	private static Object callFor(Object on, String method) {
		try {
			java.lang.reflect.Method m = null;
			for (Class<?> c = on.getClass(); c != null && m == null; c = c.getSuperclass())
				try {
					m = c.getDeclaredMethod(method);
				} catch (NoSuchMethodException ignored) {}
			if (m == null)
				throw new NoSuchMethodException(method);
			m.setAccessible(true);
			return m.invoke(on);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("couldn't call " + method, e);
		}
	}

	private static net.minecraft.world.level.block.entity.BlockEntity be(GameTestHelper helper, BlockPos pos) {
		var be = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
		if (be == null)
			throw new IllegalStateException("no block entity at " + pos + ": " + helper.getBlockState(pos));
		return be;
	}

	private static LightBlockEntity light(GameTestHelper helper, BlockPos pos) {
		if (!(be(helper, pos) instanceof LightBlockEntity l))
			throw new IllegalStateException("no light at " + pos + ": " + helper.getBlockState(pos));
		return l;
	}

	private static com.comedymonkey.createaddons.content.light.FestoonBlockEntity festoon(GameTestHelper helper, BlockPos pos) {
		if (!(be(helper, pos) instanceof com.comedymonkey.createaddons.content.light.FestoonBlockEntity f))
			throw new IllegalStateException("no Festoon hook at " + pos + ": " + helper.getBlockState(pos));
		return f;
	}

	private static com.comedymonkey.createaddons.content.electric.BatteryBlockEntity battery(GameTestHelper helper, BlockPos pos) {
		if (!(be(helper, pos) instanceof com.comedymonkey.createaddons.content.electric.BatteryBlockEntity b))
			throw new IllegalStateException("no Battery at " + pos + ": " + helper.getBlockState(pos));
		return b;
	}

	private static void named(net.minecraft.world.level.block.entity.BlockEntity be, String name) {
		be.applyComponents(net.minecraft.core.component.DataComponentMap.builder()
			.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(name)).build(),
			net.minecraft.core.component.DataComponentPatch.EMPTY);
		be.setChanged();
	}

	/** Calls a setter the game keeps to its own package (test setup only). */
	private static void call(Object on, String method, Class<?> type, Object arg) {
		call(on, method, new Class<?>[] {type}, arg);
	}

	private static void call(Object on, String method, Class<?>[] types, Object... args) {
		try {
			Class<?> c = on.getClass();
			java.lang.reflect.Method m = null;
			for (; c != null && m == null; c = c.getSuperclass())
				try {
					m = c.getDeclaredMethod(method, types);
				} catch (NoSuchMethodException ignored) {}
			if (m == null)
				throw new NoSuchMethodException(method);
			m.setAccessible(true);
			m.invoke(on, args);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("setting up: couldn't call " + method, e);
		}
	}

	/**
	 * A Copper Power Line on stone: the test course's floor isn't sturdy, and a schematic laid out in Create's schematic
	 * level has its shapes updated (as a real print does), which takes a wire off a floor that can't hold it.
	 */
	private static void floorWire(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos.below(), Blocks.STONE);
		LineGameTests.wire(helper, ModBlocks.COPPER_POWER_LINE.get(), pos, Direction.DOWN);
	}

	/** A full Lava Generator turned and mirrored as the print is (it has a front). */
	private static void generator(GameTestHelper helper, BlockPos pos, Rotation rotation, Mirror mirror) {
		helper.setBlock(pos, ModBlocks.LAVA_GENERATOR.get().defaultBlockState().mirror(mirror).rotate(rotation));
		LineGameTests.fluids(helper, pos).fill(new FluidStack(Fluids.LAVA, 8000), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
	}

	private static LineBlockEntity line(GameTestHelper helper, BlockPos pos) {
		if (!(helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof LineBlockEntity be))
			throw new IllegalStateException("no line at " + pos + ": " + helper.getBlockState(pos));
		return be;
	}

	private static LinePostBlockEntity post(GameTestHelper helper, BlockPos pos) {
		if (!(helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof LinePostBlockEntity be))
			throw new IllegalStateException("no Line Post at " + pos + ": " + helper.getBlockState(pos));
		return be;
	}

	/** The cannon spent {@code count} of exactly {@code what} (same components) in all. */
	private static void expect(GameTestHelper helper, List<ItemRequirement.StackRequirement> spent, String name, ItemStack what, int count) {
		int n = 0;
		for (var r : spent)
			if (ItemStack.isSameItemSameComponents(r.stack, what) && r.matches(what))
				n += r.stack.getCount();
		if (n != count)
			helper.fail("the cannon spent " + n + " of " + name + ", not " + count + " (" + spent.stream().map(r -> r.stack).toList() + ")");
	}

	private static String describe(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockState(pos) + (helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof LightBlockEntity l ? " " + l.phase() : "");
	}
}
