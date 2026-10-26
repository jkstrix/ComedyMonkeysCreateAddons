package com.comedymonkey.createaddons.content.electric;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Battery as an item keeps its charge: shown as a bar, in its tooltip, and in how far its tubes are lit. Placed on top
 * of an Industrial Battery (or under it), it fills the whole 3x3 layer at once, like Create's vaults, if there are
 * enough Batteries in hand and the layer is clear.
 */
public class BatteryItem extends BlockItem {
	public BatteryItem(Block block, Properties properties) {
		super(block, properties);
	}

	/** Filling a layer: the Batteries it places don't fill one of their own. */
	private static boolean filling;

	@Override
	public InteractionResult place(BlockPlaceContext context) {
		InteractionResult result = super.place(context);
		if (result.consumesAction() && !context.getLevel().isClientSide && !filling)
			fillLayer(context);
		return result;
	}

	/** The rest of the layer round the Battery just placed against a tower's top or bottom. */
	private void fillLayer(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		if (face.getAxis() != Direction.Axis.Y)
			return;
		Level level = context.getLevel();
		BlockPos placed = context.getClickedPos();
		if (!(level.getBlockEntity(placed.relative(face.getOpposite())) instanceof BatteryBlockEntity tower) || !tower.isFormed()
			|| tower.master() == null)
			return;
		BlockPos centre = BatteryTowers.center(tower.master(), tower.towerFacing()).atY(placed.getY());
		List<BlockPos> rest = new ArrayList<>();
		for (int dz = -1; dz <= 1; dz++)
			for (int dx = -1; dx <= 1; dx++) {
				BlockPos p = centre.offset(dx, 0, dz);
				if (p.equals(placed))
					continue;
				rest.add(p);
			}
		Player player = context.getPlayer();
		ItemStack stack = context.getItemInHand();
		if (player == null || player.getItemInHand(context.getHand()) != stack)
			return;
		boolean creative = player.getAbilities().instabuild;
		if (!creative && stack.getCount() < rest.size())
			return;
		// all of the layer or none: clear, the player allowed to build there, nobody standing in it
		BlockState state = getBlock().defaultBlockState();
		for (BlockPos p : rest)
			if (!level.getBlockState(p).canBeReplaced() || !level.mayInteract(player, p) || !player.mayUseItemAt(p, face, stack)
				|| !level.isUnobstructed(state, p, net.minecraft.world.phys.shapes.CollisionContext.of(player)))
				return;
		// each placed the way a player places a block (so protection and claims are asked), against the tower's layer
		filling = true;
		try {
			for (BlockPos p : rest) {
				if (stack.isEmpty())
					break;
				BlockPos against = p.relative(face.getOpposite());
				net.minecraft.world.phys.Vec3 hit = net.minecraft.world.phys.Vec3.atCenterOf(against).add(face.getStepX() * 0.5, face.getStepY() * 0.5,
					face.getStepZ() * 0.5);
				stack.useOn(new net.minecraft.world.item.context.UseOnContext(player, context.getHand(),
					new net.minecraft.world.phys.BlockHitResult(hit, face, against, false)));
			}
		} finally {
			filling = false;
		}
	}

	public static int charge(ItemStack stack) {
		return stack.getOrDefault(ModDataComponents.BATTERY_CHARGE.get(), 0);
	}

	/** How full, 0 to 1 (one from a tower may hold more than a Battery takes: full). */
	public static float fill(ItemStack stack) {
		return Mth.clamp(charge(stack) / (float) CMConfig.SERVER.batteryCapacity(), 0, 1);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return charge(stack) > 0;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13 * fill(stack));
	}

	/** The cyan of its lit tubes. */
	@Override
	public int getBarColor(ItemStack stack) {
		return 0x6FB7C4;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(Component.translatable("item.comedymonkeysaddons.battery.charge",
				String.format(Locale.ROOT, "%,d", charge(stack)), String.format(Locale.ROOT, "%,d", CMConfig.SERVER.batteryCapacity()))
			.withStyle(ChatFormatting.AQUA));
	}
}
