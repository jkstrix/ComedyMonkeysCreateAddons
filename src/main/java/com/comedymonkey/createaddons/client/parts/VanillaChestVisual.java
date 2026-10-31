package com.comedymonkey.createaddons.client.parts;

import java.util.Calendar;
import java.util.function.Consumer;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualEmbedding;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizerRegistry;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.part.InstanceTree;
import dev.engine_room.flywheel.lib.model.part.ModelTrees;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BrightnessCombiner;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * Vanilla's chests (plain, trapped, ender) as Flywheel instances, drawn exactly as vanilla's {@code ChestRenderer} draws
 * them: the same model for a single chest or either half of a double, the same texture (trapped, ender, the Christmas
 * one from 24 to 26 December), the lid and latch swung by the same eased openness, a double chest lit by the brighter of
 * its two halves, and nothing past the renderer's view distance. Standing shut, a chest costs a comparison a frame.
 * <p>
 * Flywheel tells a visual when its block's state changes but never makes it again, so a chest that joins or leaves a
 * double, or is turned, builds its instances again in {@link #update}.
 */
public final class VanillaChestVisual<T extends BlockEntity & LidBlockEntity> extends AbstractBlockEntityVisual<T> implements SimpleDynamicVisual {
	/** As vanilla's entityCutout on the chest atlas: cutout, faces culled, no mipmaps, lit as an entity. */
	private static final Material MATERIAL = SimpleMaterial.builder().texture(Sheets.CHEST_SHEET).cutout(CutoutShaders.ONE_TENTH).mipmap(false).build();

	/** Draws the three vanilla chests, unless another mod already does (then that one is left alone). */
	public static void register() {
		register(BlockEntityType.CHEST);
		register(BlockEntityType.TRAPPED_CHEST);
		register(BlockEntityType.ENDER_CHEST);
	}

	private static <T extends BlockEntity & LidBlockEntity> void register(BlockEntityType<T> type) {
		if (VisualizerRegistry.getVisualizer(type) != null)
			return;
		SimpleBlockEntityVisualizer.builder(type).factory(VanillaChestVisual<T>::new).skipVanillaRender(be -> true).apply();
	}

	/** On a ship or a contraption its position is the embedding's: the world's frustum and distance say nothing about it. */
	private final boolean embedded;
	/** The renderer's own view distance, squared: past it vanilla draws nothing, so neither does this. */
	private final double viewDistanceSqr;
	/** Christmas textures, decided once as vanilla's renderer decides them. */
	private final boolean christmas;
	private final Matrix4f pose = new Matrix4f();

	private BlockState shown;
	private InstanceTree instances;
	private InstanceTree lid, lock;
	/** How open it is (the more open of a double's halves), and its light (the brighter half's), as vanilla combines them. */
	private Float2FloatFunction openness;
	private Int2IntFunction brightness;
	private float lastAngle = Float.NaN;
	private boolean hidden;
	private volatile int light;
	private int lit = -1;

	public VanillaChestVisual(VisualizationContext context, T be, float partialTick) {
		super(context, be, partialTick);
		embedded = context instanceof VisualEmbedding;
		BlockEntityRenderer<T> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(be);
		int distance = renderer != null ? renderer.getViewDistance() : 64;
		viewDistanceSqr = (double) distance * distance;
		christmas = christmas(Calendar.getInstance());
		build(be.getBlockState());
		light = brightness.applyAsInt(computePackedLight());
		draw(partialTick);
	}

	/** Whether today is one of vanilla's Christmas chest days (24 to 26 December). */
	static boolean christmas(Calendar calendar) {
		return calendar.get(Calendar.MONTH) + 1 == 12 && calendar.get(Calendar.DAY_OF_MONTH) >= 24 && calendar.get(Calendar.DAY_OF_MONTH) <= 26;
	}

	/** The model layer vanilla draws a chest of this type with. */
	static ModelLayerLocation layer(ChestType type) {
		return switch (type) {
			case LEFT -> ModelLayers.DOUBLE_CHEST_LEFT;
			case RIGHT -> ModelLayers.DOUBLE_CHEST_RIGHT;
			case SINGLE -> ModelLayers.CHEST;
		};
	}

	/** The lid's angle for how open the chest is, eased as vanilla eases it (radians, negative is open). */
	static float lidAngle(float openness) {
		float shut = 1 - openness;
		return -((1 - shut * shut * shut) * (float) (Math.PI / 2));
	}

	private static ChestType type(BlockState state) {
		return state.hasProperty(ChestBlock.TYPE) ? state.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
	}

	/** Its instances for this block state, and how it combines with its other half. */
	private void build(BlockState state) {
		if (instances != null)
			instances.delete();
		shown = state;
		ChestType type = type(state);
		instances = InstanceTree.create(instancerProvider(),
			ModelTrees.of(layer(type), Sheets.chooseMaterial(blockEntity, type, christmas), MATERIAL));
		lid = instances.childOrThrow("lid");
		lock = instances.childOrThrow("lock");
		DoubleBlockCombiner.NeighborCombineResult<? extends BlockEntity> combined =
			state.getBlock() instanceof AbstractChestBlock<?> chest && level != null
				? chest.combine(state, level, pos, true) : DoubleBlockCombiner.Combiner::acceptNone;
		@SuppressWarnings({"unchecked", "rawtypes"}) // the combiner takes the chest's own block entity type
		Float2FloatFunction open = (Float2FloatFunction) ((DoubleBlockCombiner.NeighborCombineResult) combined).apply(ChestBlock.opennessCombiner(blockEntity));
		openness = open;
		@SuppressWarnings({"unchecked", "rawtypes"})
		Int2IntFunction bright = (Int2IntFunction) ((DoubleBlockCombiner.NeighborCombineResult) combined).apply(new BrightnessCombiner<>());
		brightness = bright;
		float facing = state.hasProperty(ChestBlock.FACING) ? state.getValue(ChestBlock.FACING).toYRot() : 0;
		pose.identity().translate(visualPos.getX(), visualPos.getY(), visualPos.getZ())
			.translate(0.5f, 0.5f, 0.5f).rotateY((float) Math.toRadians(-facing)).translate(-0.5f, -0.5f, -0.5f);
		lastAngle = Float.NaN;
		lit = -1;
		hidden = false;
		if (lightSections != null)
			lightSections.sections(sections(state));
	}

	/** Its own section, and its other half's when that's in another (vanilla lights a double by the brighter half). */
	private LongSet sections(BlockState state) {
		long own = SectionPos.asLong(pos);
		if (type(state) == ChestType.SINGLE || !state.hasProperty(ChestBlock.FACING))
			return LongSet.of(own);
		long other = SectionPos.asLong(pos.relative(ChestBlock.getConnectedDirection(state)));
		return other == own ? LongSet.of(own) : LongSet.of(own, other);
	}

	@Override
	public void setSectionCollector(SectionCollector collector) {
		lightSections = collector;
		collector.sections(sections(shown));
	}

	/** Its block state changed (joined or left a double, turned, opened): built again if it now looks different. */
	@Override
	public void update(float partialTick) {
		BlockState now = blockEntity.getBlockState();
		if (now == shown)
			return;
		if (type(now) != type(shown) || (now.hasProperty(ChestBlock.FACING) && now.getValue(ChestBlock.FACING) != shown.getValue(ChestBlock.FACING))) {
			build(now);
			light = brightness.applyAsInt(computePackedLight());
			draw(partialTick);
		} else
			shown = now;
	}

	@Override
	public boolean isVisible(FrustumIntersection frustum) {
		// a double chest's halves each draw their own block, but the lid swings up out of it
		float x = visualPos.getX(), y = visualPos.getY(), z = visualPos.getZ();
		return frustum.testAab(x, y, z, x + 1, y + 2, z + 1);
	}

	@Override
	public void beginFrame(DynamicVisual.Context context) {
		if (!embedded) {
			boolean far = pos.distToCenterSqr(context.camera().getPosition()) > viewDistanceSqr;
			if (far != hidden) {
				hidden = far;
				instances.visible(!far);
			}
			if (far || !isVisible(context.frustum()) || doDistanceLimitThisFrame(context))
				return;
		}
		draw(context.partialTick());
	}

	private void draw(float partialTick) {
		int now = light;
		if (now != lit) {
			lit = now;
			instances.traverse(i -> i.light(now).setChanged());
		}
		float angle = lidAngle(openness.get(partialTick));
		if (angle == lastAngle)
			return;
		lastAngle = angle;
		lid.xRot(angle);
		lock.xRot(angle);
		instances.updateInstances(pose);
	}

	@Override
	public void updateLight(float partialTick) {
		// (applied on the next frame: Flywheel runs light updates alongside it)
		light = brightness.applyAsInt(computePackedLight());
	}

	@Override
	public void collectCrumblingInstances(Consumer<Instance> consumer) {
		instances.traverse(consumer);
	}

	@Override
	protected void _delete() {
		instances.delete();
	}
}
