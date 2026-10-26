package com.comedymonkey.createaddons.client.line;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.joml.Matrix4f;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.parts.PartModels;
import com.comedymonkey.createaddons.content.line.LineColors;
import com.comedymonkey.createaddons.content.line.LinePostBlock;
import com.comedymonkey.createaddons.content.line.LinePostBlockEntity;
import com.comedymonkey.createaddons.content.line.LineTier;
import com.comedymonkey.createaddons.content.line.Span;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.SectionTrackedVisual.SectionCollector;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The spans a Line Post owns, as Flywheel instances: each a run of short straight pieces of cable (one model, a unit
 * box with the line's body texture, stretched into place and tinted its tier or dye), sagging like
 * {@link LinePostRenderer}'s. Nothing moves, so nothing is done a frame: they're laid out when the visual is made, again
 * when its spans change or a far post comes into view, and lit again only when the light where they hang changes.
 */
public class LinePostVisual extends AbstractBlockEntityVisual<LinePostBlockEntity> {
	public static final ModelResourceLocation SEGMENT = ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/line_fitting/span_segment"));
	private static final int SEGMENTS = 16;

	private final List<TransformedInstance> segments = new ArrayList<>();
	/** Where each segment takes its light from (its middle), and the sections those are in (whose light it follows). */
	private final List<BlockPos> lightAt = new ArrayList<>();
	private LongSet sections = new LongOpenHashSet();
	/** What it was last laid out from (its spans and the far posts' states): the same again, nothing is done. */
	private List<Object> laidOutFrom = null;

	public LinePostVisual(dev.engine_room.flywheel.api.visualization.VisualizationContext context, LinePostBlockEntity be, float partialTick) {
		super(context, be, partialTick);
		layOut();
	}

	/** Its spans changed, or a far post it hangs to came into view. */
	@Override
	public void update(float partialTick) {
		layOut();
	}

	private void layOut() {
		if (level == null)
			return;
		BlockPos pos = blockEntity.getBlockPos();
		List<Object> from = new ArrayList<>();
		for (Span s : blockEntity.spans())
			if (s.owner()) {
				from.add(s);
				from.add(level.getBlockState(pos.offset(s.offset())));
			}
		if (from.equals(laidOutFrom))
			return;
		laidOutFrom = from;
		segments.forEach(TransformedInstance::delete);
		segments.clear();
		lightAt.clear();
		Vec3 origin = Vec3.atLowerCornerOf(pos), render = Vec3.atLowerCornerOf(visualPos);
		LongSet sections = new LongOpenHashSet();
		sections.add(SectionPos.asLong(pos));
		for (Span s : blockEntity.spans()) {
			if (!s.owner())
				continue;
			BlockPos farPos = pos.offset(s.offset());
			BlockState far = level.getBlockState(farPos);
			if (!(far.getBlock() instanceof LinePostBlock))
				continue;
			Vec3 a = blockEntity.tie(s.from()).subtract(origin), b = LinePostBlock.tie(far, farPos, s.to()).subtract(origin);
			boolean covered = s.dye() != null, brass = s.tier() == LineTier.BRASS;
			float w = (covered ? (brass ? 1.35f : 1.05f) : (brass ? 0.9f : 0.6f)) / 16, h = (covered ? 1.05f : brass ? 0.5f : 0.6f) / 16;
			int color = LineColors.of(s.tier(), s.dye()) | 0xFF000000;
			double sag = Math.max(0.5, a.distanceTo(b) / 24);
			Vec3 prev = a;
			for (int i = 1; i <= SEGMENTS; i++) {
				double t = i / (double) SEGMENTS;
				Vec3 p = a.lerp(b, t).subtract(0, 4 * sag * t * (1 - t), 0);
				BlockPos mid = BlockPos.containing(prev.add(p).scale(0.5).add(origin));
				TransformedInstance seg = instancerProvider().instancer(InstanceTypes.TRANSFORMED, PartModels.get(SEGMENT, RenderType.cutout()))
					.createInstance();
				seg.setTransform(segment(prev.add(render), p.add(render), w, h));
				seg.colorArgb(color);
				seg.light(LevelRenderer.getLightColor(level, mid));
				seg.setChanged();
				segments.add(seg);
				lightAt.add(mid);
				sections.add(SectionPos.asLong(mid));
				prev = p;
			}
		}
		this.sections = sections;
		if (lightSections != null)
			lightSections.sections(sections);
	}

	/** (Flywheel hands it over after the visual is made: the sections its spans hang through, from then on.) */
	@Override
	public void setSectionCollector(SectionCollector collector) {
		super.setSectionCollector(collector);
		collector.sections(sections);
	}

	/** The unit box stretched from p to q, w wide and h high about the line between them (as the renderer's tube). */
	private static Matrix4f segment(Vec3 p, Vec3 q, float w, float h) {
		Vec3 dir = q.subtract(p);
		Vec3 along = dir.normalize();
		Vec3 side = along.cross(new Vec3(0, 1, 0));
		if (side.lengthSqr() < 1e-4)
			side = along.cross(new Vec3(1, 0, 0));
		side = side.normalize();
		Vec3 up = side.cross(along).normalize();
		Vec3 o = p.subtract(up.scale(h / 2)).subtract(side.scale(w / 2));
		return new Matrix4f(
			(float) dir.x, (float) dir.y, (float) dir.z, 0,
			(float) (up.x * h), (float) (up.y * h), (float) (up.z * h), 0,
			(float) (side.x * w), (float) (side.y * w), (float) (side.z * w), 0,
			(float) o.x, (float) o.y, (float) o.z, 1);
	}

	@Override
	public void updateLight(float partialTick) {
		for (int i = 0; i < segments.size(); i++) {
			segments.get(i).light(LevelRenderer.getLightColor(level, lightAt.get(i)));
			segments.get(i).setChanged();
		}
	}

	@Override
	public void collectCrumblingInstances(Consumer<Instance> consumer) {}

	@Override
	protected void _delete() {
		segments.forEach(TransformedInstance::delete);
		segments.clear();
		lightAt.clear();
	}
}
