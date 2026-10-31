package com.comedymonkey.createaddons.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.joml.Vector4f;
import org.joml.Vector4fc;

import com.comedymonkey.createaddons.content.light.FestoonBlockEntity;
import com.comedymonkey.createaddons.content.light.FestoonHookBlock;
import com.comedymonkey.createaddons.content.light.LightBlockEntity;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.SectionTrackedVisual.SectionCollector;
import dev.engine_room.flywheel.api.visualization.VisualEmbedding;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.ModelUtil;
import dev.engine_room.flywheel.lib.model.QuadMesh;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.item.DyeColor;

/**
 * The strings a Festoon hook holds, as Flywheel instances: every rod {@link FestoonRenderer} draws (laid out by the same
 * {@link FestoonGeometry}) is an instance of one rod model per texture, stretched into place. They're laid out when the
 * visual is made and again when its strings change; a frame only asks whether the bulbs are lit (they flicker as the run
 * starves) and their colour, and changes the bulbs when that changed. Each bulb has a lit and an unlit instance, one of
 * them shown. Past the renderer's view distance they're hidden, as the renderer drew nothing there.
 * <p>
 * Only {@link #beginFrame} (and the constructor) touches the instances: Flywheel may run light updates alongside the
 * frame, so {@link #updateLight} only works out the new light for the next frame.
 */
public final class FestoonVisual extends AbstractBlockEntityVisual<FestoonBlockEntity> implements SimpleDynamicVisual {
	/** The rod model in each texture (flex, steel, unlit glass, lit glass), made once per resource reload. */
	private static final Map<Integer, Model> MODELS = new ConcurrentHashMap<>();
	private static final int FLEX = 0, STEEL = 1, GLASS_OFF = 2, GLASS_LIT = 3;

	/** On a ship or a contraption its position is relative to that: the camera distance says nothing about it. */
	private final boolean embedded;
	/** Each rod's instance, and each bulb's lit one (its unlit one is its rod's), by rod; null for other rods. */
	private final List<TransformedInstance> rods = new ArrayList<>();
	private final List<TransformedInstance> litBulbs = new ArrayList<>();
	/** What it was laid out from: the same again, nothing is done. */
	private BlockPos laidAt;
	private Direction laidFacing;
	private List<FestoonBlockEntity.Span> laidSpans;
	/** (read by light updates, on other threads) */
	private volatile FestoonGeometry.Layout layout;
	private LongSet sections = new LongOpenHashSet();
	/** Its strings may have changed (set by an update, from any thread; looked at next frame). */
	private volatile boolean stale;
	/** New light for these rods, from a light update, for the next frame. */
	private final AtomicReference<Relit> relit = new AtomicReference<>();
	/** The light each rod was last given (a lit bulb's own is full-bright, not this). */
	private int[] lights = new int[0];
	/** What it shows now: anything at all (in range), the bulbs lit, and their colour lit; and whether the flex shows. */
	private boolean showing = true, lit = false, flexShown = true;
	private int glow = -1;
	@org.jetbrains.annotations.Nullable
	private DyeColor glowDye;
	private int glowStep = -1;

	private record Relit(FestoonGeometry.Layout layout, int[] lights) {}

	public FestoonVisual(VisualizationContext context, FestoonBlockEntity be, float partialTick) {
		super(context, be, partialTick);
		embedded = context instanceof VisualEmbedding;
		layOut();
	}

	/** After the models are baked again (Flywheel makes its visuals again then too). */
	public static void forget() {
		MODELS.clear();
	}

	/** Its strings changed (or its block did). */
	@Override
	public void update(float partialTick) {
		stale = true;
	}

	@Override
	public void beginFrame(DynamicVisual.Context context) {
		if (stale) {
			stale = false;
			layOut();
		}
		Relit light = relit.getAndSet(null);
		if (light != null)
			relight(light.layout() == layout ? light.lights() : lightsOf(layout));
		boolean show = embedded || pos.distToCenterSqr(context.camera().getPosition()) <= (double) FestoonRenderer.VIEW_DISTANCE * FestoonRenderer.VIEW_DISTANCE;
		boolean on = show && blockEntity.phase() != LightBlockEntity.Phase.DARK && LightRenderer.lit(blockEntity, level.getGameTime(), context.partialTick());
		int colour = glow;
		if (on && (glow < 0 || blockEntity.dye() != glowDye || blockEntity.step() != glowStep)) {
			glowDye = blockEntity.dye();
			glowStep = blockEntity.step();
			colour = LightRenderer.runGlow(glowDye, glowStep);
		}
		if (show != showing || on != lit || colour != glow) {
			showing = show;
			lit = on;
			glow = colour;
			show();
		}
	}

	/** Lays the rods out again, if what they're laid out from changed. */
	private void layOut() {
		BlockPos at = blockEntity.getBlockPos();
		Direction facing = blockEntity.getBlockState().getValue(FestoonHookBlock.FACING);
		List<FestoonBlockEntity.Span> spans = blockEntity.spans();
		if (layout != null && facing == laidFacing && at.equals(laidAt) && spans.equals(laidSpans))
			return;
		laidAt = at.immutable();
		laidFacing = facing;
		laidSpans = List.copyOf(spans);
		deleteInstances();
		layout = FestoonGeometry.layOut(laidAt, facing, laidSpans);
		float x = visualPos.getX(), y = visualPos.getY(), z = visualPos.getZ();
		for (int i = 0; i < layout.rods(); i++) {
			FestoonGeometry.Part part = layout.parts()[i];
			TransformedInstance rod = instance(part == FestoonGeometry.Part.FLEX ? FLEX : part == FestoonGeometry.Part.SOCKET ? STEEL : GLASS_OFF);
			rod.setTransform(layout.matrix(i, x, y, z));
			rods.add(rod);
			TransformedInstance litBulb = null;
			if (part == FestoonGeometry.Part.BULB) {
				litBulb = instance(GLASS_LIT);
				litBulb.setTransform(layout.matrix(i, x, y, z));
				litBulb.light(LightTexture.FULL_BRIGHT);
			}
			litBulbs.add(litBulb);
		}
		LongSet sections = new LongOpenHashSet();
		sections.add(SectionPos.asLong(at));
		for (long cell : layout.cells())
			sections.add(SectionPos.asLong(BlockPos.of(cell)));
		this.sections = sections;
		if (lightSections != null)
			lightSections.sections(sections);
		lights = new int[layout.rods()];
		java.util.Arrays.fill(lights, -1);
		relight(lightsOf(layout));
		// (new instances show)
		flexShown = true;
		show();
	}

	private TransformedInstance instance(int texture) {
		return instancerProvider().instancer(InstanceTypes.TRANSFORMED, model(texture)).createInstance();
	}

	/** Each rod's light: the light where it hangs. */
	private int[] lightsOf(FestoonGeometry.Layout layout) {
		long[] cells = layout.cells();
		int[] byCell = new int[cells.length];
		BlockPos.MutableBlockPos cell = new BlockPos.MutableBlockPos();
		for (int i = 0; i < cells.length; i++)
			byCell[i] = LevelRenderer.getLightColor(level, cell.set(cells[i]));
		int[] out = new int[layout.rods()];
		for (int i = 0; i < out.length; i++)
			out[i] = byCell[layout.cell()[i]];
		return out;
	}

	private void relight(int[] light) {
		for (int i = 0; i < rods.size(); i++)
			if (lights[i] != light[i]) {
				lights[i] = light[i];
				rods.get(i).light(light[i]);
				rods.get(i).setChanged();
			}
	}

	/** Shows the flex and sockets while in range, and each bulb lit (full-bright, in its colour) or unlit. */
	private void show() {
		boolean flex = flexShown != showing;
		flexShown = showing;
		for (int i = 0; i < rods.size(); i++) {
			TransformedInstance rod = rods.get(i), litBulb = litBulbs.get(i);
			if (litBulb == null) {
				if (flex) {
					rod.setVisible(showing);
					rod.setChanged();
				}
				continue;
			}
			rod.setVisible(showing && !lit);
			rod.setChanged();
			litBulb.setVisible(showing && lit);
			if (lit)
				litBulb.colorRgb(glow);
			litBulb.setChanged();
		}
	}

	@Override
	public void updateLight(float partialTick) {
		FestoonGeometry.Layout now = layout;
		if (now != null)
			relit.set(new Relit(now, lightsOf(now)));
	}

	/** (Flywheel hands it over after the visual is made: the sections its strings hang through, from then on.) */
	@Override
	public void setSectionCollector(SectionCollector collector) {
		super.setSectionCollector(collector);
		collector.sections(sections);
	}

	/** Mining the hook cracks its strings too, as the renderer's did. */
	@Override
	public void collectCrumblingInstances(Consumer<Instance> consumer) {
		rods.forEach(consumer);
		for (TransformedInstance litBulb : litBulbs)
			if (litBulb != null)
				consumer.accept(litBulb);
	}

	private void deleteInstances() {
		rods.forEach(TransformedInstance::delete);
		for (TransformedInstance litBulb : litBulbs)
			if (litBulb != null)
				litBulb.delete();
		rods.clear();
		litBulbs.clear();
	}

	@Override
	protected void _delete() {
		deleteInstances();
	}

	/** The rod in this texture: unshaded cutout, as the renderer's rods are. */
	private static Model model(int texture) {
		return MODELS.computeIfAbsent(texture, t -> new SingleMeshModel(new RodMesh(FestoonRenderer.sprites()[t]),
			ModelUtil.getMaterial(RenderType.cutout(), false, false)));
	}

	/** {@link FestoonGeometry#ROD} in one texture, as a Flywheel mesh: white, unlit (its instance's light is used). */
	private static final class RodMesh implements QuadMesh {
		/** Around the rod shape's middle, reaching its corners. */
		private static final Vector4fc BOUNDS = new Vector4f(0.5f, 0, 0, 1.5f);
		private final TextureAtlasSprite sprite;

		RodMesh(TextureAtlasSprite sprite) {
			this.sprite = sprite;
		}

		@Override
		public int vertexCount() {
			return FestoonGeometry.ROD_VERTICES;
		}

		@Override
		public void write(MutableVertexList out) {
			float[] rod = FestoonGeometry.ROD;
			for (int k = 0; k < FestoonGeometry.ROD_VERTICES; k++) {
				int p = k * FestoonGeometry.ROD_STRIDE;
				out.x(k, rod[p]);
				out.y(k, rod[p + 1]);
				out.z(k, rod[p + 2]);
				out.r(k, 1);
				out.g(k, 1);
				out.b(k, 1);
				out.a(k, 1);
				out.u(k, sprite.getU(rod[p + 6]));
				out.v(k, sprite.getV(rod[p + 7]));
				out.overlay(k, OverlayTexture.NO_OVERLAY);
				out.light(k, 0);
				out.normalX(k, rod[p + 3]);
				out.normalY(k, rod[p + 4]);
				out.normalZ(k, rod[p + 5]);
			}
		}

		@Override
		public Vector4fc boundingSphere() {
			return BOUNDS;
		}
	}
}
