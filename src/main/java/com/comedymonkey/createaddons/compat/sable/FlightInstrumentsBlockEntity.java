package com.comedymonkey.createaddons.compat.sable;

import java.util.List;
import java.util.Locale;

import org.joml.Quaterniondc;
import org.joml.Vector3d;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Reads the ship it's on: altitude, airspeed, climb rate, heading, pitch and roll. */
public class FlightInstrumentsBlockEntity extends BlockEntity implements IHaveGoggleInformation {
	private static final String[] POINTS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

	/** One snapshot of the instruments. Speeds in blocks (metres) per second, angles in degrees. */
	public record Readings(double altitude, double airspeed, double climb, double heading, double pitch, double roll,
						   boolean onShip) {
		public String compassPoint() {
			return POINTS[(int) Math.round(heading / 45) & 7];
		}

		/** "045° NE": whole degrees, wrapping so that just short of 360 reads 000 rather than 360. */
		public String headingText() {
			return String.format(Locale.ROOT, "%03d° %s", Math.floorMod(Math.round(heading), 360), compassPoint());
		}
	}

	public FlightInstrumentsBlockEntity(BlockPos pos, BlockState state) {
		super(AeronauticsContent.FLIGHT_INSTRUMENTS_BE.get(), pos, state);
	}

	public Readings readings() {
		return read(level, worldPosition, getBlockState().getValue(FlightInstrumentsBlock.FACING));
	}

	/** Works on either side; on a ship it reads the ship's pose, otherwise just where the block stands. */
	public static Readings read(Level level, BlockPos pos, Direction facing) {
		Vec3 centre = Vec3.atCenterOf(pos);
		// The pilot faces the panel, so the ship's forward points from the pilot into the panel
		Vector3d forward = new Vector3d(-facing.getStepX(), 0, -facing.getStepZ());
		Vector3d right = new Vector3d(forward).cross(0, 1, 0);
		SubLevelAccess ship = level == null ? null : Sable.HELPER.getContaining(level, pos);
		if (ship == null)
			return new Readings(centre.y, 0, 0, headingOf(forward), 0, 0, false);

		Quaterniondc orientation = ship.logicalPose().orientation();
		orientation.transform(forward);
		orientation.transform(right);
		Vec3 world = Sable.HELPER.projectOutOfSubLevel(level, centre);
		Vec3 velocity = Sable.HELPER.getVelocity(level, centre);
		double pitch = Math.toDegrees(Math.asin(clamp(forward.y)));
		double roll = Math.toDegrees(Math.asin(clamp(-right.y)));
		return new Readings(world.y, velocity.length(), velocity.y, headingOf(forward), pitch, roll, true);
	}

	private static double clamp(double v) {
		return Math.max(-1, Math.min(1, v));
	}

	/** Compass heading of a direction: north (-Z) is 0, east (+X) is 90. */
	private static double headingOf(Vector3d direction) {
		double deg = Math.toDegrees(Math.atan2(direction.x, -direction.z));
		return (deg + 360) % 360;
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		Readings r = readings();
		lang().translate("gui.goggles.flight_instruments").forGoggles(tooltip);
		line(tooltip, "altitude", String.format(Locale.ROOT, "%.1f m", r.altitude()));
		line(tooltip, "airspeed", String.format(Locale.ROOT, "%.1f m/s", r.airspeed()));
		line(tooltip, "climb", String.format(Locale.ROOT, "%+.1f m/s", r.climb()));
		line(tooltip, "heading", r.headingText());
		line(tooltip, "attitude", String.format(Locale.ROOT, "%+.0f° / %+.0f°", r.pitch(), r.roll()));
		if (!r.onShip())
			lang().translate("gui.goggles.flight_instruments.grounded").style(ChatFormatting.DARK_GRAY).forGoggles(tooltip, 1);
		return true;
	}

	private static void line(List<Component> tooltip, String key, String value) {
		lang().translate("gui.goggles.flight_instruments." + key).style(ChatFormatting.GRAY)
			.add(lang().text(" " + value).style(ChatFormatting.AQUA)).forGoggles(tooltip, 1);
	}

	private static LangBuilder lang() {
		return new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID);
	}
}
