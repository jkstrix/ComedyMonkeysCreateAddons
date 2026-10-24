package com.comedymonkey.createaddons.content;

import com.comedymonkey.createaddons.config.CMConfig;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;

/**
 * Villagers sitting in Create Seats. Vanilla rides a villager from its feet, where a player rides from 0.6 blocks up,
 * so on a seat a villager stands on the cushion. With the setting on, it is lowered by the player's 0.6 (see
 * {@code SeatEntityMixin}, on both sides so the server and client agree where it is) and its legs are bent forward
 * like a player's (see {@code VillagerModelMixin}).
 */
public final class VillagerSeats {
	/** Player.DEFAULT_VEHICLE_ATTACHMENT, so a villager's hips sit where a player's do. */
	public static final double SEAT_OFFSET = -0.6;

	private VillagerSeats() {}

	/** Whether this passenger of a seat (on the ground or on a contraption) is lowered to sit in it. */
	public static boolean sits(Entity passenger) {
		return passenger instanceof AbstractVillager && CMConfig.SERVER.villagersSitInSeats();
	}

	/** Whether this entity is sitting in a seat right now. Every passenger of a contraption is in one of its seats. */
	public static boolean sitting(Entity entity) {
		Entity vehicle = entity.getVehicle();
		return (vehicle instanceof SeatEntity || vehicle instanceof AbstractContraptionEntity) && sits(entity);
	}
}
