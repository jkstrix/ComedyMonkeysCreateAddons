package com.comedymonkey.createaddons.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.content.pen.AnimalPenBlockEntity;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.testutil.ReflectionAccess;
import com.comedymonkey.createaddons.testutil.TestConfig;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.core.BlockPos;

/** The Animal Pens draw no animals in a shader's shadow pass, and all of them in Ponder. */
class AnimalPenRendererTest {
	private static AnimalPenBlockEntity pen;

	@BeforeAll
	static void setUp() {
		TestConfig.loadDefaults();
		pen = new AnimalPenBlockEntity(BlockPos.ZERO, ModBlocks.ANIMAL_PEN.get().defaultBlockState());
	}

	private static CameraView view(String which) {
		return (CameraView) ReflectionAccess.get(CameraView.of(pen, new PoseStack()), which);
	}

	private static int detail(AnimalPenRenderer renderer, CameraView view) {
		return (Integer) ReflectionAccess.call(renderer, "detail", pen, view);
	}

	@Test
	void noAnimalsInAShadowPass() {
		AnimalPenRenderer renderer = new AnimalPenRenderer(null);
		assertEquals(ReflectionAccess.get(renderer, "NONE"), detail(renderer, view("SHADOW")));
		// and it doesn't count as what was drawn last time
		assertTrue(((Map<?, ?>) ReflectionAccess.get(renderer, "detail")).isEmpty());
	}

	@Test
	void allAnimalsInPonder() {
		AnimalPenRenderer renderer = new AnimalPenRenderer(null);
		assertEquals(ReflectionAccess.get(renderer, "ALL"), detail(renderer, view("PONDER")));
	}

	@Test
	void farOffNone() {
		AnimalPenRenderer renderer = new AnimalPenRenderer(null);
		PoseStack ms = new PoseStack();
		ms.translate(-200, 0, 0);
		assertEquals(ReflectionAccess.get(renderer, "NONE"), detail(renderer, CameraView.of(pen, ms)));
	}
}
