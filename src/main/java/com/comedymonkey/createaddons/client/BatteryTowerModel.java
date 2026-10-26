package com.comedymonkey.createaddons.client;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.electric.BatteryBlock;
import com.comedymonkey.createaddons.content.electric.BatteryBlockEntity;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.math.Transformation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * A formed Battery's model: its tile of the Industrial Battery, by the tower's height and the block's layer and place
 * in it, turned to the tower's front. The tiles are written by design/models/battery.export.js, which lists them in
 * assets/comedymonkeysaddons/battery_tower.json (height → layer → row from the front → column from the west); each is
 * an ordinary block model, so it's lit and culled like any block, and only what moves is left to the renderer.
 */
public class BatteryTowerModel extends BakedModelWrapper<BakedModel> {
	private static final ResourceLocation INDEX = ComedyMonkeysCreateAddons.asResource("battery_tower.json");
	/** Tile model names by height, layer, row and column (null: nothing there). */
	private static volatile Map<Integer, String[][][]> TILES = Map.of();
	private static final Map<String, List<BakedQuad>> CACHE = new ConcurrentHashMap<>();
	/** Bumped on each bake, so quads worked out from the last models while it was clearing aren't kept. */
	private static volatile int generation;
	private static final ChunkRenderTypeSet CUTOUT = ChunkRenderTypeSet.of(RenderType.cutout());

	public BatteryTowerModel(BakedModel original) {
		super(original);
	}

	/** Every tile the index names, so they're baked. */
	public static void registerModels(ModelEvent.RegisterAdditional event) {
		TILES = readIndex(Minecraft.getInstance().getResourceManager());
		Set<String> names = new LinkedHashSet<>();
		for (String[][][] grid : TILES.values())
			for (String[][] layer : grid)
				for (String[] row : layer)
					for (String name : row)
						if (name != null)
							names.add(name);
		for (String name : names)
			event.register(tile(name));
	}

	/** The formed states draw their tile. */
	public static void wrap(ModelEvent.ModifyBakingResult event) {
		for (BlockState state : ModBlocks.BATTERY.get().getStateDefinition().getPossibleStates()) {
			if (!state.getValue(BatteryBlock.FORMED))
				continue;
			ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
			BakedModel model = event.getModels().get(location);
			if (model != null && !(model instanceof BatteryTowerModel))
				event.getModels().put(location, new BatteryTowerModel(model));
		}
	}

	/** The tiles are read again after a resource reload. */
	public static void forget() {
		generation++;
		CACHE.clear();
	}

	private static ModelResourceLocation tile(String name) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/" + name));
	}

	private static Map<Integer, String[][][]> readIndex(ResourceManager resources) {
		Map<Integer, String[][][]> tiles = new HashMap<>();
		try (Reader reader = resources.openAsReader(INDEX)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			for (Map.Entry<String, JsonElement> e : root.entrySet()) {
				JsonArray layers = e.getValue().getAsJsonArray();
				String[][][] grid = new String[layers.size()][3][3];
				for (int layer = 0; layer < layers.size(); layer++)
					for (int iz = 0; iz < 3; iz++)
						for (int ix = 0; ix < 3; ix++) {
							JsonElement n = layers.get(layer).getAsJsonArray().get(iz).getAsJsonArray().get(ix);
							grid[layer][iz][ix] = n.isJsonNull() ? null : n.getAsString();
						}
				tiles.put(Integer.parseInt(e.getKey()), grid);
			}
		} catch (Exception e) {
			com.mojang.logging.LogUtils.getLogger().error("Couldn't read the Industrial Battery's tiles from {}", INDEX, e);
		}
		return tiles;
	}

	/** Where the world gives no model data of its own (Ponder, schematics), the tile is read from the block entity. */
	@Override
	public ModelData getModelData(net.minecraft.world.level.BlockAndTintGetter level, net.minecraft.core.BlockPos pos, BlockState state, ModelData data) {
		data = super.getModelData(level, pos, state, data);
		if (data.get(BatteryBlockEntity.TILE) == null && level.getBlockEntity(pos) instanceof BatteryBlockEntity be && be.tile() != null)
			data = data.derive().with(BatteryBlockEntity.TILE, be.tile()).build();
		return data;
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data,
									@Nullable RenderType renderType) {
		BatteryBlockEntity.Tile t = data.get(BatteryBlockEntity.TILE);
		if (t == null || side != null || (renderType != null && renderType != RenderType.cutout()))
			return List.of();
		String[][][] grid = TILES.get(t.height());
		if (grid == null || t.layer() >= grid.length)
			return List.of();
		String name = grid[t.layer()][t.iz()][t.ix()];
		if (name == null)
			return List.of();
		return CACHE.computeIfAbsent(generation + ";" + name + ";" + t.facing().getSerializedName(), k -> turned(name, t.facing()));
	}

	@Override
	public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
		return CUTOUT;
	}

	/** A tile's quads, turned from facing north to face the tower's front, about the block's middle. */
	private static List<BakedQuad> turned(String name, Direction facing) {
		BakedModel model = Minecraft.getInstance().getModelManager().getModel(tile(name));
		List<BakedQuad> quads = new ArrayList<>();
		RandomSource rand = RandomSource.create(42);
		for (Direction s : Direction.values())
			quads.addAll(model.getQuads(null, s, rand, ModelData.EMPTY, null));
		quads.addAll(model.getQuads(null, null, rand, ModelData.EMPTY, null));
		if (facing == Direction.NORTH)
			return quads;
		Matrix3f m = new Matrix3f().rotateY((float) Math.toRadians(-(facing.toYRot() + 180)));
		IQuadTransformer turn = QuadTransformers.applying(new Transformation(
			new Matrix4f().translate(0.5f, 0.5f, 0.5f).mul(new Matrix4f(m)).translate(-0.5f, -0.5f, -0.5f)));
		List<BakedQuad> out = new ArrayList<>(quads.size());
		// each turned quad faces its new way, so it's shaded for it
		for (BakedQuad q : turn.process(quads)) {
			Vector3f n = m.transform(new Vector3f(q.getDirection().getStepX(), q.getDirection().getStepY(), q.getDirection().getStepZ()));
			out.add(new BakedQuad(q.getVertices(), q.getTintIndex(), Direction.getNearest(n.x, n.y, n.z), q.getSprite(), q.isShade(),
				q.hasAmbientOcclusion()));
		}
		return out;
	}
}
