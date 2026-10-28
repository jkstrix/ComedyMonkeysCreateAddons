package com.comedymonkey.createaddons.registry;

import java.util.function.Supplier;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.juggernaut.ruin.RuinField;
import com.comedymonkey.createaddons.content.juggernaut.ruin.RuinHall;
import com.comedymonkey.createaddons.content.juggernaut.ruin.JuggernautRuinStructure;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Juggernaut Ruins (D31, D55): their structure type and its two kinds of piece. */
public final class ModStructures {
	public static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, ComedyMonkeysCreateAddons.MOD_ID);
	public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, ComedyMonkeysCreateAddons.MOD_ID);

	public static final Supplier<StructureType<JuggernautRuinStructure>> JUGGERNAUT_RUIN = TYPES.register("juggernaut_ruin",
		() -> () -> JuggernautRuinStructure.CODEC);
	/** The hall at the middle, where he kneels (a structure template). */
	public static final Supplier<StructurePieceType> RUIN_HALL = PIECES.register("juggernaut_ruin_hall",
		() -> (StructurePieceType.StructureTemplateType) RuinHall::new);
	/** The broken walls and fallen blocks round it, thinning out to the odd block at the edge. */
	public static final Supplier<StructurePieceType> RUIN_FIELD = PIECES.register("juggernaut_ruin_field",
		() -> (StructurePieceType.ContextlessType) RuinField::new);

	private ModStructures() {}
}
