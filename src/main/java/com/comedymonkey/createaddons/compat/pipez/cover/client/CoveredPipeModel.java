package com.comedymonkey.createaddons.compat.pipez.cover.client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.compat.pipez.cover.PipeCover;
import com.comedymonkey.createaddons.compat.pipez.cover.PipeCoverShapes;
import com.simibubi.create.foundation.model.BakedModelHelper;
import com.simibubi.create.foundation.model.BakedQuadHelper;

import de.maxhenkel.pipez.blocks.PipeBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * Wraps every Pipez pipe model. A covered pipe is drawn as its cover instead, each face cut from the matching face of
 * the cover's block, the way Create's copycats are. A see-through cover (glass, say) still shows the pipe inside.
 */
public class CoveredPipeModel extends BakedModelWrapper<BakedModel> {
	private static final ModelProperty<Look> LOOK = new ModelProperty<>();

	/** What the mesher needs: the block to copy, and the faces to cut from it. */
	private record Look(BlockState material, boolean opaque, List<Face> faces) {}

	/** One face of one cover box; {@code culled} faces lie on the block's side and go out with the neighbour's. */
	private record Face(AABB box, Direction side, boolean culled) {}

	public CoveredPipeModel(BakedModel original) {
		super(original);
	}

	public static void wrapPipes(ModelEvent.ModifyBakingResult event) {
		for (Block block : BuiltInRegistries.BLOCK)
			if (block instanceof PipeBlock)
				for (BlockState state : block.getStateDefinition().getPossibleStates()) {
					ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
					BakedModel model = event.getModels().get(location);
					if (model != null && !(model instanceof CoveredPipeModel))
						event.getModels().put(location, new CoveredPipeModel(model));
				}
	}

	@Override
	public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
		data = super.getModelData(level, pos, state, data);
		PipeCover cover = PipeCoverShapes.cover(level, pos);
		if (cover == null)
			return data;
		List<Face> faces = new ArrayList<>();
		for (PipeCoverShapes.Box box : PipeCoverShapes.boxes(cover.kind(), state, level, pos))
			for (Direction side : Direction.values())
				if (!box.hidden().contains(side))
					faces.add(new Face(box.box(), side, onBlockSide(box.box(), side)));
		return data.derive().with(LOOK, new Look(cover.appearance(), cover.isOpaque(), faces)).build();
	}

	private static boolean onBlockSide(AABB box, Direction side) {
		return switch (side) {
			case DOWN -> box.minY <= 0;
			case UP -> box.maxY >= 1;
			case NORTH -> box.minZ <= 0;
			case SOUTH -> box.maxZ >= 1;
			case WEST -> box.minX <= 0;
			case EAST -> box.maxX >= 1;
		};
	}

	private static BakedModel modelOf(BlockState material) {
		return Minecraft.getInstance().getBlockRenderer().getBlockModel(material);
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data,
									@Nullable RenderType renderType) {
		Look look = data.get(LOOK);
		if (look == null)
			return super.getQuads(state, side, rand, data, renderType);
		List<BakedQuad> quads = new ArrayList<>();
		if (!look.opaque())
			quads.addAll(super.getQuads(state, side, rand, data, renderType));
		BakedModel material = modelOf(look.material());
		if (renderType != null && !material.getRenderTypes(look.material(), rand, ModelData.EMPTY).contains(renderType))
			return quads;
		for (Face face : look.faces()) {
			if (face.culled() ? face.side() != side : side != null)
				continue;
			for (BakedQuad quad : materialFace(material, look.material(), face.side(), rand, renderType))
				quads.add(cut(quad, face));
		}
		return quads;
	}

	/** The block's quads facing {@code side}, whether it lists them under that side or as unculled. */
	private static List<BakedQuad> materialFace(BakedModel model, BlockState state, Direction side, RandomSource rand,
												@Nullable RenderType renderType) {
		List<BakedQuad> quads = new ArrayList<>(model.getQuads(state, side, rand, ModelData.EMPTY, renderType));
		for (BakedQuad quad : model.getQuads(state, null, rand, ModelData.EMPTY, renderType))
			if (quad.getDirection() == side)
				quads.add(quad);
		return quads;
	}

	/** Crops a full block face to this face of the box and moves it out (or in) to the box's side. */
	private static BakedQuad cut(BakedQuad quad, Face face) {
		AABB box = face.box();
		Direction.Axis axis = face.side().getAxis();
		AABB crop = switch (axis) {
			case X -> new AABB(0, box.minY, box.minZ, 1, box.maxY, box.maxZ);
			case Y -> new AABB(box.minX, 0, box.minZ, box.maxX, 1, box.maxZ);
			case Z -> new AABB(box.minX, box.minY, 0, box.maxX, box.maxY, 1);
		};
		boolean positive = face.side().getAxisDirection() == Direction.AxisDirection.POSITIVE;
		double offset = positive ? box.max(axis) - 1 : box.min(axis);
		Vec3 move = new Vec3(axis == Direction.Axis.X ? offset : 0, axis == Direction.Axis.Y ? offset : 0,
			axis == Direction.Axis.Z ? offset : 0);
		TextureAtlasSprite sprite = quad.getSprite();
		return BakedQuadHelper.cloneWithCustomGeometry(quad,
			BakedModelHelper.cropAndMove(quad.getVertices(), sprite, crop, move));
	}

	@Override
	public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
		Look look = data.get(LOOK);
		if (look == null)
			return super.getRenderTypes(state, rand, data);
		ChunkRenderTypeSet material = modelOf(look.material()).getRenderTypes(look.material(), rand, ModelData.EMPTY);
		return look.opaque() ? material : ChunkRenderTypeSet.union(material, super.getRenderTypes(state, rand, data));
	}

	@Override
	public TextureAtlasSprite getParticleIcon(ModelData data) {
		Look look = data.get(LOOK);
		return look == null ? super.getParticleIcon(data) : modelOf(look.material()).getParticleIcon(ModelData.EMPTY);
	}
}
