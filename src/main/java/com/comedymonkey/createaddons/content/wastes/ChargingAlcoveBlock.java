package com.comedymonkey.createaddons.content.wastes;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.util.StringRepresentable;

/**
 * The Charging Alcove (D37, brief §1): decor three tall that drops itself, whose contact plate lights with starlight
 * while its district is fed, as the city's lamps do. Each of its three blocks lights its own part of the plate
 * ({@link DerelictLightBlockEntity}; the lit plate is an emissive part of each block's model). One a player places
 * belongs to no city and stays dark. It casts no light.
 * <p>
 * D72: a small cast corner shelf in its middle block, holding a {@link #TRINKET} (nothing, a brass cog, a winding key
 * standing on its socket, the makers' toy cart, a pressed flower in a little brass frame), and {@link #VINES}, dead vines down
 * its back and cheek and one strand hanging in its opening. Both are drawn by its blockstate on every part (the trinket in
 * the middle block, the vines in the middle and top), set by the city; one placed by a player has none of either.
 */
public class ChargingAlcoveBlock extends ThreePartBlock implements EntityBlock, DerelictLightBlockEntity.DerelictLight {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	public static final EnumProperty<Trinket> TRINKET = EnumProperty.create("trinket", Trinket.class);
	public static final BooleanProperty VINES = BooleanProperty.create("vines");

	private final double[][][] boxes;

	public ChargingAlcoveBlock(Properties properties, double[][][] northBoxes) {
		super(properties, false, northBoxes);
		this.boxes = northBoxes;
		registerDefaultState(defaultBlockState().setValue(LIT, false).setValue(TRINKET, Trinket.NONE).setValue(VINES, false));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return simpleCodec(p -> new ChargingAlcoveBlock(p, boxes));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(LIT, TRINKET, VINES);
	}

	@Override
	public boolean lightable(BlockState state) {
		return true;
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof DerelictLightBlockEntity be)
			be.scheduledTick();
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DerelictLightBlockEntity(pos, state);
	}

	/** What its corner shelf holds (D72; world brief: a cog, a winding key, a toy cart, a pressed flower). */
	public enum Trinket implements StringRepresentable {
		NONE, COG, KEY, CART, FLOWER;

		/** The four things a shelf can hold. */
		public static final Trinket[] KINDS = { COG, KEY, CART, FLOWER };

		@Override
		public String getSerializedName() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}
}
