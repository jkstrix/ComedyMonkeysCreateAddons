package com.comedymonkey.createaddons.client.line;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.line.LineBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * A line's block model, wrapped: an encased line is drawn as the block it's encased in, whole, and the line inside not
 * at all (Engineer's Goggles show where it runs). Lines that aren't encased draw as they are.
 */
public class EncasedLineModel extends BakedModelWrapper<BakedModel> {
	static final ModelProperty<BlockState> ENCASING = new ModelProperty<>();
	/** The casing model's own data here (its connected textures, say). */
	static final ModelProperty<ModelData> CASING_DATA = new ModelProperty<>();

	public EncasedLineModel(BakedModel original) {
		super(original);
	}

	@Override
	public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
		data = super.getModelData(level, pos, state, data);
		if (level.getBlockEntity(pos) instanceof LineBlockEntity be && be.encasing() != null)
			return data.derive().with(ENCASING, be.encasing())
				.with(CASING_DATA, modelOf(be.encasing()).getModelData(level, pos, be.encasing(), ModelData.EMPTY)).build();
		return data;
	}

	@Nullable
	static BlockState encasing(ModelData data) {
		return data.get(ENCASING);
	}

	private static ModelData casingData(ModelData data) {
		ModelData d = data.get(CASING_DATA);
		return d == null ? ModelData.EMPTY : d;
	}

	private static BakedModel modelOf(BlockState state) {
		return Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data,
									@Nullable RenderType renderType) {
		BlockState casing = encasing(data);
		if (casing == null)
			return lineQuads(state, side, rand, data, renderType);
		BakedModel model = modelOf(casing);
		ModelData casingData = casingData(data);
		if (renderType != null && !model.getRenderTypes(casing, rand, casingData).contains(renderType))
			return List.of();
		return model.getQuads(casing, side, rand, casingData, renderType);
	}

	/** The line's own quads (overridden by lines whose model is built from the block entity). */
	protected List<BakedQuad> lineQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data,
										@Nullable RenderType renderType) {
		return super.getQuads(state, side, rand, data, renderType);
	}

	@Override
	public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
		BlockState casing = encasing(data);
		return casing == null ? super.getRenderTypes(state, rand, data) : modelOf(casing).getRenderTypes(casing, rand, casingData(data));
	}

	@Override
	public TextureAtlasSprite getParticleIcon(ModelData data) {
		BlockState casing = encasing(data);
		return casing == null ? super.getParticleIcon(data) : modelOf(casing).getParticleIcon(ModelData.EMPTY);
	}
}
