package com.comedymonkey.createaddons.content.wastes;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Taking a broken machine or bot shell apart (design/specs/clockwork-wastes.md, D26, D32, P16, P43). A Create Wrench
 * gives the full salvage: the block comes apart and drops its {@code salvage/<id>} loot table (Create parts and
 * Clockwork Scrap, or scrap and a chance of an Electron Tube). The same for a player's sneak-click and a Deployer's
 * click, through {@link #salvage}. Broken any other way (pickaxe, drill, saw, blast), the block drops its ordinary
 * {@code blocks/<id>} table: one scrap. Both tables are data, so packs can change them.
 */
public final class Salvage {
	private Salvage() {}

	/** The full-salvage loot table of a salvage block: {@code comedymonkeysaddons:salvage/<id>}. */
	public static ResourceKey<LootTable> table(Block block) {
		return ResourceKey.create(Registries.LOOT_TABLE,
			ComedyMonkeysCreateAddons.asResource("salvage/" + BuiltInRegistries.BLOCK.getKey(block).getPath()));
	}

	/** Whether this tool takes a salvage block apart: Create's Wrench. */
	public static boolean isWrench(ItemStack stack) {
		return stack.getItem() instanceof WrenchItem;
	}

	/** Whether a click comes from a Deployer (Create acts as a fake player), which salvages without sneaking. */
	public static boolean isDeployer(@Nullable Player player) {
		return player instanceof DeployerFakePlayer;
	}

	/**
	 * Takes the salvage block at {@code pos} apart: rolls its salvage table, removes it with the break effect, and
	 * pops the parts out where it stood.
	 * @return the stacks dropped, or an empty list if the block there isn't a salvage block
	 */
	public static List<ItemStack> salvage(ServerLevel level, BlockPos pos, @Nullable Player player, ItemStack tool) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof SalvageBlock))
			return List.of();
		LootTable table = level.getServer().reloadableRegistries().getLootTable(table(state.getBlock()));
		LootParams params = new LootParams.Builder(level)
			.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
			.withParameter(LootContextParams.TOOL, tool)
			.withParameter(LootContextParams.BLOCK_STATE, state)
			.withOptionalParameter(LootContextParams.THIS_ENTITY, player)
			.create(LootContextParamSets.BLOCK);
		List<ItemStack> drops = table.getRandomItems(params);
		// the break particles and sound, then the block gone without its ordinary (scrap) drop
		level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
		level.removeBlock(pos, false);
		for (ItemStack drop : drops)
			Block.popResource(level, pos, drop);
		return drops;
	}
}
