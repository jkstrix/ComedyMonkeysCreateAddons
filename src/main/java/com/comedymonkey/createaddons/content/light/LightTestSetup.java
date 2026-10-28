package com.comedymonkey.createaddons.content.light;

import java.util.ArrayList;
import java.util.List;

import com.comedymonkey.createaddons.content.line.FluidLineBlock;
import com.comedymonkey.createaddons.content.line.FluidLineBlockEntity;
import com.comedymonkey.createaddons.content.line.PowerLineBlock;
import com.comedymonkey.createaddons.content.wellspring.WellspringBlockEntity;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModItems;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * {@code /lights testsetup}: builds an Electric Lights showcase street next to you (a dev and play-test aid, op only).
 * A lava Wellspring with three Dripstone Upgrades feeds a Lava Generator through a Fluid Line (50 mB/s in, 20 burnt), so
 * it runs for good; a copper Power Line runs east from it down the middle of the street, between two walls. Beside the
 * line, every light: plain on the south side, dyed on the north; then a Bollard in each of the 16 dyes, then a Bulkhead
 * and a Globe Head at each brightness step. Every light is on the floor beside the line, so ceiling lights hang from a
 * block at head height; each has a sign with its name on the wall behind.
 */
public final class LightTestSetup {
	/** The street's floor, its walls (two blocks out from the line; white, so the light's colour shows) and the blocks ceiling lights hang from. */
	private static final BlockState FLOOR = Blocks.POLISHED_ANDESITE.defaultBlockState(), WALL = Blocks.WHITE_CONCRETE.defaultBlockState(),
		BEAM = Blocks.DARK_OAK_PLANKS.defaultBlockState();
	/** The dyes the north row of lights takes, in turn. */
	private static final DyeColor[] ROW_DYES = {DyeColor.RED, DyeColor.LIGHT_BLUE, DyeColor.LIME, DyeColor.ORANGE, DyeColor.PURPLE,
		DyeColor.CYAN, DyeColor.YELLOW, DyeColor.PINK, DyeColor.BLUE, DyeColor.GREEN, DyeColor.MAGENTA, DyeColor.WHITE, DyeColor.BLACK};
	private static final int SPACING = 3;
	/** The width of each brightness step's bay. */
	private static final int BAY = 7;
	/** Blocks of floor strip in each run, before it turns up the wall. */
	private static final int STRIP_RUN = 5;

	private LightTestSetup() {}

	public static void register(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("lights").requires(source -> source.hasPermission(2))
			.then(Commands.literal("testsetup").executes(LightTestSetup::build)));
	}

	private static int build(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		ServerLevel level = source.getLevel();
		BlockPos o = BlockPos.containing(source.getPosition());
		int kinds = LightKind.values().length, dyes = DyeColor.values().length / 2, steps = LightKind.STEPS.length;
		int length = 2 + SPACING * (kinds + dyes) + BAY * steps + 1 + STRIP_RUN + 3;
		street(level, o, length);
		power(level, o);

		List<BlockPos> wires = new ArrayList<>();
		for (int x = 1; x < length; x++) {
			BlockPos p = o.offset(x, 0, 0);
			level.setBlock(p, ModBlocks.COPPER_POWER_LINE.get().defaultBlockState().setValue(PowerLineBlock.FACES.get(Direction.DOWN), true),
				Block.UPDATE_ALL);
			wires.add(p);
		}

		int x = 2;
		for (LightKind kind : LightKind.values()) {
			DyeColor dye = ROW_DYES[kind.ordinal() % ROW_DYES.length];
			light(level, o.offset(x, 0, 1), kind, Direction.NORTH, null, 0, name(kind), "");
			light(level, o.offset(x, 0, -1), kind, Direction.SOUTH, dye, 0, name(kind), dyeName(dye));
			x += SPACING;
		}
		DyeColor[] all = DyeColor.values();
		for (int i = 0; i < dyes; i++) {
			light(level, o.offset(x, 0, 1), LightKind.BOLLARD, Direction.NORTH, all[i], 0, dyeName(all[i]), "");
			light(level, o.offset(x, 0, -1), LightKind.BOLLARD, Direction.SOUTH, all[i + dyes], 0, dyeName(all[i + dyes]), "");
			x += SPACING;
		}
		// each brightness step in a bay of its own, walled off from the next (a doorway over the line), so its light shows
		// alone and not with its neighbours'
		String[] stepNames = {"Full", "Bright", "Dim", "Glow"};
		x += BAY / 2 - 1;
		for (int s = 0; s < steps; s++) {
			divider(level, o.offset(x - BAY / 2, 0, 0));
			light(level, o.offset(x, 0, 1), LightKind.BULKHEAD, Direction.NORTH, null, s, "Brightness", stepNames[s]);
			light(level, o.offset(x, 0, -1), LightKind.GLOBE_HEAD, Direction.SOUTH, null, s, "Brightness", stepNames[s]);
			x += BAY;
		}
		divider(level, o.offset(x - BAY / 2, 0, 0));
		x -= BAY / 2 - 1;

		// Light Strips: a run along the floor beside the line, turning up the wall at its far end; plain south, dyed north
		stripRun(level, o.offset(x, 0, 1), Direction.SOUTH, null);
		stripRun(level, o.offset(x, 0, -1), Direction.NORTH, DyeColor.LIGHT_BLUE);
		sign(level, o.offset(x, 1, 1), Direction.NORTH, "Light Strip", "");
		sign(level, o.offset(x, 1, -1), Direction.SOUTH, "Light Strip", "Light Blue");
		x += STRIP_RUN + 1;

		// the line links up once its blocks are all in; again a moment later, as the game tests do
		wires.forEach(p -> PowerLineBlock.relinkAround(level, p));
		MinecraftServer server = level.getServer();
		server.tell(new TickTask(server.getTickCount() + 2, () -> wires.forEach(p -> PowerLineBlock.relinkAround(level, p))));
		int built = kinds * 2 + dyes * 2 + steps * 2;
		source.sendSuccess(() -> Component.literal("Built the Electric Lights test street: " + built + " lights along +x, powered by a lava "
			+ "Wellspring and a Lava Generator at its west end. Plain lights on the south side, dyed on the north."), true);
		return 1;
	}

	/**
	 * A Light Strip run from {@code start} along +x on the floor, turning up the street's wall (towards {@code wall}) at its
	 * last block and on up it; fed where it passes the line. Dyed the whole run if {@code dye} isn't null.
	 */
	private static void stripRun(ServerLevel level, BlockPos start, Direction wall, DyeColor dye) {
		BlockState strip = ModBlocks.LIGHT_STRIP.get().defaultBlockState();
		for (int i = 0; i < STRIP_RUN; i++) {
			BlockState s = strip.setValue(LightStripBlock.FACES.get(Direction.DOWN), true);
			// the last turns up the wall in the same block (an inside corner)
			if (i == STRIP_RUN - 1)
				s = s.setValue(LightStripBlock.FACES.get(wall), true);
			level.setBlock(start.east(i), s, Block.UPDATE_ALL);
		}
		for (int y = 1; y <= 2; y++)
			level.setBlock(start.east(STRIP_RUN - 1).above(y), strip.setValue(LightStripBlock.FACES.get(wall), true), Block.UPDATE_ALL);
		if (dye != null && level.getBlockEntity(start) instanceof LightStripBlockEntity be)
			for (LightStripBlockEntity piece : be.run())
				piece.setDye(dye);
	}

	/** A wall across the street at x, three high, with a doorway one wide and two high over the line. */
	private static void divider(ServerLevel level, BlockPos at) {
		for (int z = -1; z <= 1; z++)
			for (int y = 0; y <= 2; y++)
				if (z != 0 || y == 2)
					level.setBlock(at.offset(0, y, z), WALL, Block.UPDATE_ALL);
	}

	/** Clears the street's space and lays its floor and walls (two blocks out from the line, three high). */
	private static void street(ServerLevel level, BlockPos o, int length) {
		for (int x = -4; x <= length + 1; x++)
			for (int z = -4; z <= 4; z++) {
				level.setBlock(o.offset(x, -1, z), FLOOR, Block.UPDATE_CLIENTS);
				for (int y = 0; y <= 5; y++)
					level.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
				if (x >= 1 && x < length && Math.abs(z) == 2)
					for (int y = 0; y <= 2; y++)
						level.setBlock(o.offset(x, y, z), WALL, Block.UPDATE_CLIENTS);
			}
	}

	/**
	 * The Lava Generator at the street's west end, facing north (power out of its east and west sides), and behind it, to
	 * the south, a Fluid Line into its inlet from the upgraded lava Wellspring.
	 */
	private static void power(ServerLevel level, BlockPos o) {
		BlockPos gen = o, pipe = o.south(), spring = o.south(2);
		level.setBlock(spring, ModBlocks.WELLSPRING.get().defaultBlockState(), Block.UPDATE_ALL);
		if (level.getBlockEntity(spring) instanceof WellspringBlockEntity w)
			for (int slot = 0; slot < 3; slot++)
				w.getUpgrades().insertItem(slot, new ItemStack(ModItems.DRIPSTONE_UPGRADE.get()), false);
		level.setBlock(gen, ModBlocks.LAVA_GENERATOR.get().defaultBlockState(), Block.UPDATE_ALL);
		// a full tank to start on while the Wellspring's lava comes down the line
		var tank = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, gen, null);
		if (tank != null)
			tank.fill(new FluidStack(Fluids.LAVA, 8000), IFluidHandler.FluidAction.EXECUTE);
		level.setBlock(pipe, ModBlocks.COPPER_FLUID_LINE.get().defaultBlockState(), Block.UPDATE_ALL);
		FluidLineBlock.refresh(level, pipe);
		if (level.getBlockEntity(pipe) instanceof FluidLineBlockEntity line)
			line.setIntake(Direction.SOUTH);
	}

	/**
	 * A light on the floor beside the line at {@code pos}, facing the line ({@code facing}), with what it needs to hang
	 * from or be fixed to, dyed and stepped, and a sign on the wall behind it, one block along.
	 */
	private static void light(ServerLevel level, BlockPos pos, LightKind kind, Direction facing, DyeColor dye, int step, String line1,
							  String line2) {
		Direction back = facing.getOpposite();
		switch (kind.mount()) {
			case CEILING -> level.setBlock(pos.above(), BEAM, Block.UPDATE_ALL);
			// wall and face lights on a post of wall between them and the street's wall
			case WALL, ANY -> level.setBlock(pos.relative(back), WALL, Block.UPDATE_ALL);
			default -> {}
		}
		BlockState state = ModBlocks.LIGHTS.get(kind).get().defaultBlockState();
		if (kind.mount().horizontal() || kind.mount().anyWay())
			state = state.setValue(LightBlock.FACING, facing);
		level.setBlock(pos, state, Block.UPDATE_ALL);
		if (level.getBlockEntity(pos) instanceof LightBlockEntity be) {
			if (dye != null)
				be.setDye(dye);
			for (int s = 0; s < step; s++)
				be.stepBrightness();
		}
		// on the street's wall, one block along and at head height
		sign(level, pos.east().above(), facing, line1, line2);
	}

	/** A dark oak sign at {@code at} on the wall behind it, facing out, with a name (wrapped) and a note under it. */
	private static void sign(ServerLevel level, BlockPos at, Direction facing, String name, String note) {
		level.setBlock(at, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing), Block.UPDATE_ALL);
		if (!(level.getBlockEntity(at) instanceof SignBlockEntity sign))
			return;
		List<String> lines = wrap(name, 15);
		SignText text = new SignText().setHasGlowingText(true).setColor(DyeColor.WHITE);
		for (int i = 0; i < Math.min(3, lines.size()); i++)
			text = text.setMessage(i, Component.literal(lines.get(i)));
		if (!note.isEmpty())
			text = text.setMessage(Math.min(3, lines.size()), Component.literal(note));
		sign.setText(text, true);
	}

	private static List<String> wrap(String s, int width) {
		List<String> out = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : s.split(" ")) {
			if (line.length() > 0 && line.length() + 1 + word.length() > width) {
				out.add(line.toString());
				line.setLength(0);
			}
			if (line.length() > 0)
				line.append(' ');
			line.append(word);
		}
		if (line.length() > 0)
			out.add(line.toString());
		return out;
	}

	private static String name(LightKind kind) {
		return Language.getInstance().getOrDefault(ModBlocks.LIGHTS.get(kind).get().getDescriptionId());
	}

	private static String dyeName(DyeColor dye) {
		return Language.getInstance().getOrDefault("color.minecraft." + dye.getName());
	}
}
