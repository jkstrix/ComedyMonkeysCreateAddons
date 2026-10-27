package com.comedymonkey.createaddons.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.content.cage.MonsterCageBlockEntity;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.testutil.ReflectionAccess;
import com.comedymonkey.createaddons.testutil.TestConfig;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.core.BlockPos;

/**
 * How many of a window's creatures and items are drawn: by the camera's distance (worked out from the pose, as a
 * renderer is given it, with hysteresis), shared evenly when the windows ask for more than a frame allows, none in a
 * shader's shadow pass, all in Ponder and outside a frame (the website's renders).
 */
class CameraViewTest {
	private static int next;

	@BeforeAll
	static void setUp() {
		TestConfig.loadDefaults();
	}

	/** A fresh frame with nothing asked of the last one. */
	@BeforeEach
	void freshFrame() {
		CameraView.startFrame();
		CameraView.startFrame();
	}

	@AfterEach
	void endFrame() {
		CameraView.endFrame();
	}

	/** A window of its own, so no other test's last tier carries over. */
	private static MonsterCageBlockEntity window() {
		return new MonsterCageBlockEntity(new BlockPos(next++, 0, 0), ModBlocks.MONSTER_CAGE.get().defaultBlockState());
	}

	/** The view of {@code window} from a camera {@code blocks} from its centre, along x. */
	private static CameraView at(MonsterCageBlockEntity window, double blocks) {
		PoseStack ms = new PoseStack();
		// the pose's origin is the camera: the block sits at minus the camera's place
		ms.translate(-(0.5 + blocks), -0.5, -0.5);
		return CameraView.of(window, ms);
	}

	private static CameraView at(double blocks) {
		return at(window(), blocks);
	}

	private static CameraView constant(String which) {
		return (CameraView) ReflectionAccess.get(at(1), which);
	}

	@Test
	void theDistanceComesFromThePose() {
		CameraView view = at(12);
		assertTrue(view.known());
		assertEquals(144, view.distanceSqr(), 1e-3);
	}

	@Test
	void creaturesStepDownWithDistance() {
		assertEquals(4, at(2).creatures(4));
		assertEquals(4, at(9.9).creatures(4));
		assertEquals(2, at(10.1).creatures(4));
		assertEquals(2, at(15.9).creatures(4));
		assertEquals(1, at(16.1).creatures(4));
		assertEquals(1, at(31.9).creatures(4));
		assertEquals(0, at(32.1).creatures(4));
		// never more than there are
		assertEquals(1, at(12).creatures(1));
		assertEquals(3, at(5).creatures(3));
	}

	/** Going out a tier drops at its edge; coming back it returns only a block inside, so sneaking at the edge doesn't pop them. */
	@Test
	void tiersHaveHysteresis() {
		MonsterCageBlockEntity window = window();
		assertEquals(4, at(window, 9.8).creatures(4));
		assertEquals(2, at(window, 10.2).creatures(4));
		assertEquals(2, at(window, 9.8).creatures(4), "back just inside 10: still two");
		assertEquals(2, at(window, 9.1).creatures(4));
		assertEquals(4, at(window, 8.9).creatures(4), "a block inside: all four");
		assertEquals(0, at(window, 32.5).creatures(4));
		assertEquals(0, at(window, 31.5).creatures(4));
		assertEquals(1, at(window, 30.9).creatures(4));
		// the tier itself, without a window, has no memory
		assertEquals(0, CameraView.tier(null, 9.8 * 9.8));
	}

	@Test
	void itemsStepDownWithDistance() {
		assertEquals(3, at(5).items(3));
		assertEquals(3, at(15.9).items(3));
		assertEquals(1, at(16.1).items(3));
		assertEquals(1, at(31.9).items(3));
		assertEquals(0, at(32.1).items(3));
		assertEquals(0, at(40).items(1));
	}

	/** Items step down at 16 and 32 blocks and come back a block inside each, as the creatures do. */
	@Test
	void itemTiersHaveHysteresis() {
		MonsterCageBlockEntity window = window();
		assertEquals(3, at(window, 15.8).items(3));
		assertEquals(1, at(window, 16.2).items(3));
		assertEquals(1, at(window, 15.8).items(3), "back just inside 16: still one");
		assertEquals(1, at(window, 15.1).items(3));
		assertEquals(3, at(window, 14.9).items(3), "a block inside: all three");
		assertEquals(0, at(window, 32.5).items(3));
		assertEquals(0, at(window, 31.5).items(3));
		assertEquals(1, at(window, 30.9).items(3));
		// the creature and item tiers are kept apart
		MonsterCageBlockEntity both = window();
		assertEquals(2, at(both, 12).creatures(4));
		assertEquals(3, at(both, 12).items(3));
		// the tier itself, without a window, has no memory
		assertEquals(0, CameraView.itemTier(null, 15.8 * 15.8));
	}

	@Test
	void noneInAShadowPass() {
		CameraView view = constant("SHADOW");
		assertTrue(view.shadow());
		assertFalse(view.known());
		assertEquals(0, view.creatures(4));
		assertEquals(0, view.items(3));
	}

	@Test
	void allInPonder() {
		CameraView view = constant("PONDER");
		assertFalse(view.shadow());
		assertEquals(4, view.creatures(4));
		assertEquals(3, view.items(3));
	}

	/** Drawn outside a frame (the website's renders, 24 blocks off): everything, and nothing counted against the frames. */
	@Test
	void allOutsideAFrame() {
		CameraView.endFrame();
		for (int window = 0; window < 40; window++) {
			CameraView view = at(24);
			assertEquals(4, view.creatures(4));
			assertEquals(3, view.items(3));
		}
		CameraView.startFrame();
		CameraView.startFrame();
		for (int window = 0; window < 16; window++)
			assertEquals(4, at(3).creatures(4), "the renders asked nothing of the next frames");
	}

	/** Within the budget, every window draws its tier. */
	@Test
	void underTheBudgetNothingIsHeldBack() {
		for (int window = 0; window < 16; window++)
			at(3).creatures(4);
		CameraView.startFrame();
		for (int window = 0; window < 16; window++)
			assertEquals(4, at(3).creatures(4));
	}

	/**
	 * Over the budget, every window gets the same share, whatever order they're drawn in: the cage 2 blocks away isn't
	 * left empty for one 12 blocks off, and the first and last drawn look alike.
	 */
	@Test
	void overTheBudgetEveryWindowGetsAnEvenShare() {
		MonsterCageBlockEntity[] windows = new MonsterCageBlockEntity[30];
		for (int i = 0; i < windows.length; i++)
			windows[i] = window();
		// last frame: 30 windows close up, wanting 120
		for (MonsterCageBlockEntity window : windows)
			at(window, 3).creatures(4);
		CameraView.startFrame();
		// this frame: 64 / 30 = 2 each, near and far, first and last
		for (int i = 0; i < windows.length; i++)
			assertEquals(2, at(windows[i], i == 0 ? 2 : i == windows.length - 1 ? 12 : 3).creatures(4), "window " + i);
	}

	/** With more windows wanting creatures than there are creatures to share, near ones keep one each and the rest none. */
	@Test
	void withTooManyWindowsTheNearOnesKeepOne() {
		for (int i = 0; i < 100; i++)
			at(3).creatures(4);
		CameraView.startFrame();
		assertEquals(1, at(3).creatures(4));
		assertEquals(0, at(12).creatures(4));
		assertEquals(0, at(20).creatures(4));
		assertEquals(4, constant("PONDER").creatures(4), "Ponder isn't shared out");
	}

	@Test
	void shadowPassesAskForNothing() {
		for (int pass = 0; pass < 100; pass++)
			constant("SHADOW").creatures(4);
		CameraView.startFrame();
		assertEquals(4, at(3).creatures(4));
	}
}
