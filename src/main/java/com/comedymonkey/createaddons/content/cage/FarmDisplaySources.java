package com.comedymonkey.createaddons.content.cage;

import java.util.Locale;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.tank.FishTankBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * What a Display Link can read off the Monster Cages and Fish Tanks (D68): the horde or school ("Blaze: 112 / 256"),
 * what it's doing ("Next one 40%", "Needs Monster Chow"), the water in a Flooded cage or a tank, and the Netherite cage's
 * containment and escape clock.
 */
public final class FarmDisplaySources {
	private FarmDisplaySources() {}

	private static MutableComponent tr(String key, Object... args) {
		return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".display_source." + key, args);
	}

	/** A line that can carry a label, as the other sources' do. */
	private abstract static class Line extends SingleLineDisplaySource {
		@Override
		protected boolean allowsLabeling(DisplayLinkContext context) {
			return true;
		}
	}

	/** "Blaze: 112 / 256", or "Cod: 40 + 6 young / 128". */
	public static class Count extends Line {
		@Override
		protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
			if (context.getSourceBlockEntity() instanceof MonsterCageBlockEntity cage)
				return cage.hasMonster() ? cage.getHorde().copy() : cage.getStatus().copy();
			if (context.getSourceBlockEntity() instanceof FishTankBlockEntity tank)
				return tank.hasCreature() ? tank.getSchool().copy() : tank.getStatus().copy();
			return EMPTY_LINE;
		}
	}

	/** What it's doing: "Next one 40%", "Needs Monster Chow", "Breeding: 60%", "Needs water to breed". */
	public static class Status extends Line {
		@Override
		protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
			if (context.getSourceBlockEntity() instanceof MonsterCageBlockEntity cage)
				return cage.getStatus().copy();
			if (context.getSourceBlockEntity() instanceof FishTankBlockEntity tank)
				return tank.getStatus().copy();
			return EMPTY_LINE;
		}
	}

	/** "Water: 2,000 / 4,000 mB" in a Flooded cage or a tank. */
	public static class Water extends Line {
		@Override
		protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
			FluidTank water = context.getSourceBlockEntity() instanceof MonsterCageBlockEntity cage && cage.isFlooded() ? cage.getWater()
				: context.getSourceBlockEntity() instanceof FishTankBlockEntity tank ? tank.getWater() : null;
			if (water == null)
				return EMPTY_LINE;
			return tr("farm_water.line", String.format(Locale.ROOT, "%,d / %,d mB", water.getFluidAmount(), water.getCapacity()));
		}
	}

	/** The Netherite cage's field: "Containment field up", the countdown, or the escape clock. */
	public static class Containment extends Line {
		@Override
		protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
			if (!(context.getSourceBlockEntity() instanceof MonsterCageBlockEntity cage) || !cage.isNetherite())
				return EMPTY_LINE;
			return cage.getContainment().copy();
		}
	}
}
