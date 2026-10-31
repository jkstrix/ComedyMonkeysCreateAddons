package com.comedymonkey.createaddons.client.juggernaut;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.client.parts.PartSink;
import com.comedymonkey.createaddons.content.juggernaut.SpringEngineBlock;
import com.comedymonkey.createaddons.content.juggernaut.SpringEngineBlockEntity;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.testutil.ReflectionAccess;
import com.comedymonkey.createaddons.testutil.TestConfig;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.core.BlockPos;

/** JUG-02: however overfull an engine claims to be, the renderer picks frames that exist (an index past them crashes the client). */
class SpringEngineRendererTest {
	@BeforeEach
	void config() {
		TestConfig.loadDefaults();
	}

	@AfterEach
	void restore() {
		TestConfig.reset();
	}

	private static List<String> parts(float shownFill) {
		SpringEngineBlockEntity engine = new SpringEngineBlockEntity(BlockPos.ZERO,
			ModBlocks.SPRING_ENGINE.get().defaultBlockState().setValue(SpringEngineBlock.PART, SpringEngineBlock.Part.OUTPUT));
		ReflectionAccess.set(engine, "shownFill", shownFill);
		List<String> drawn = new ArrayList<>();
		PartSink sink = (model, ms, light, type, rgb) -> drawn.add(model.id().getPath());
		assertDoesNotThrow(() -> SpringEngineRenderer.parts(engine, 0, new PoseStack(), sink, 0), "at " + shownFill);
		return drawn;
	}

	@Test
	void anOverfullEngineDrawsAsFull() {
		for (float fill : new float[] { 1.2f, 2f, 8f }) {
			List<String> drawn = parts(fill);
			assertTrue(drawn.contains("block/spring_engine_coil_front_3"), "full coil at " + fill + ": " + drawn);
			assertTrue(drawn.contains("block/spring_engine_runes_4"), "all runes lit at " + fill + ": " + drawn);
		}
	}

	@Test
	void aNegativeFillDrawsAsEmpty() {
		List<String> drawn = parts(-0.5f);
		assertTrue(drawn.contains("block/spring_engine_coil_front_0"), drawn.toString());
		assertTrue(drawn.stream().noneMatch(p -> p.startsWith("block/spring_engine_runes")), drawn.toString());
	}
}
