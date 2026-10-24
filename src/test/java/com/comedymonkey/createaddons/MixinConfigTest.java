package com.comedymonkey.createaddons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * The mixin config, and the Pipez methods its mixins inject into. Every injection is {@code require = 0} so a Pipez
 * update that renames one fails silently in game; these checks catch that against the Pipez on the classpath. The
 * mixin classes themselves must never be loaded directly, so only their class files are looked for. (Nor can this test
 * live in the mixin package: Mixin refuses to load any class there.)
 */
class MixinConfigTest {
	private static final String CONFIG = "comedymonkeysaddons.mixins.json";
	private static final String PACKAGE = "com.comedymonkey.createaddons.mixin";
	private static JsonObject config;

	@BeforeAll
	static void readConfig() throws IOException {
		try (InputStream in = loader().getResourceAsStream(CONFIG)) {
			assertNotNull(in, CONFIG + " isn't on the classpath");
			config = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	private static ClassLoader loader() {
		return MixinConfigTest.class.getClassLoader();
	}

	@Test
	void configIsOptionalAndTargetsJava21() {
		// Pipez is optional, so a missing target must not stop the game
		assertFalse(config.get("required").getAsBoolean());
		assertEquals(PACKAGE, config.get("package").getAsString());
		assertEquals("JAVA_21", config.get("compatibilityLevel").getAsString());
		assertEquals(0, config.getAsJsonObject("injectors").get("defaultRequire").getAsInt());
	}

	@Test
	void everyListedMixinExists() {
		List<String> all = new ArrayList<>(names("mixins"));
		all.addAll(names("client"));
		assertFalse(all.isEmpty());
		for (String name : all) {
			String resource = (PACKAGE + "." + name).replace('.', '/') + ".class";
			assertNotNull(loader().getResource(resource), "no class for mixin " + name);
		}
	}

	@Test
	void noMixinIsListedTwice() {
		List<String> all = new ArrayList<>(names("mixins"));
		all.addAll(names("client"));
		assertEquals(new HashSet<>(all).size(), all.size(), all.toString());
	}

	@Test
	void rendererMixinIsClientOnly() {
		assertTrue(names("client").contains("PipeRendererMixin"));
		assertFalse(names("mixins").contains("PipeRendererMixin"));
		assertTrue(names("mixins").containsAll(Set.of("PipeBlockMixin", "PipeTileEntityMixin")));
	}

	/** The Cuckoo Clock's restock times work on the server, so its villager mixin goes on both sides. */
	@Test
	void villagerMixinIsCommon() {
		assertTrue(names("mixins").contains("VillagerMixin"));
		assertFalse(names("client").contains("VillagerMixin"));
	}

	@Test
	void villagerHasShouldRestockAndTheCountersItKeeps() throws Exception {
		Class<?> villager = Class.forName("net.minecraft.world.entity.npc.Villager", false, loader());
		assertEquals(boolean.class, villager.getDeclaredMethod("shouldRestock").getReturnType());
		// the mixin's @Shadow fields
		assertEquals(int.class, villager.getDeclaredField("numberOfRestocksToday").getType());
		assertEquals(long.class, villager.getDeclaredField("lastRestockCheckDayTime").getType());
	}

	/** Where a seated villager is placed must agree on both sides; only its pose is drawn on the client. */
	@Test
	void seatMixinIsCommonAndModelMixinClientOnly() {
		assertTrue(names("mixins").contains("SeatEntityMixin"));
		assertFalse(names("client").contains("SeatEntityMixin"));
		assertTrue(names("client").contains("VillagerModelMixin"));
		assertFalse(names("mixins").contains("VillagerModelMixin"));
	}

	@Test
	void seatEntityHasGetCustomEntitySeatOffset() throws Exception {
		Class<?> seat = Class.forName("com.simibubi.create.content.contraptions.actors.seat.SeatEntity", false, loader());
		Class<?> entity = Class.forName("net.minecraft.world.entity.Entity", false, loader());
		assertEquals(double.class, seat.getDeclaredMethod("getCustomEntitySeatOffset", entity).getReturnType());
	}

	@Test
	void villagerModelHasSetupAnimAndItsLegs() throws Exception {
		Class<?> model, entity, part;
		try {
			model = Class.forName("net.minecraft.client.model.VillagerModel", false, loader());
			entity = Class.forName("net.minecraft.world.entity.Entity", false, loader());
			part = Class.forName("net.minecraft.client.model.geom.ModelPart", false, loader());
		} catch (LinkageError | ClassNotFoundException | RuntimeException e) {
			Assumptions.abort("client classes aren't available here: " + e);
			return;
		}
		model.getDeclaredMethod("setupAnim", entity, float.class, float.class, float.class, float.class, float.class);
		// the mixin's @Shadow fields
		assertEquals(part, model.getDeclaredField("rightLeg").getType());
		assertEquals(part, model.getDeclaredField("leftLeg").getType());
	}

	@Test
	void pipeBlockHasEveryInjectedMethod() throws Exception {
		Class<?> pipe = pipez("de.maxhenkel.pipez.blocks.PipeBlock");
		pipe.getDeclaredMethod("getShape", BlockState.class, BlockGetter.class, BlockPos.class, CollisionContext.class);
		pipe.getDeclaredMethod("getCollisionShape", BlockState.class, BlockGetter.class, BlockPos.class, CollisionContext.class);
		pipe.getDeclaredMethod("getInteractionShape", BlockState.class, BlockGetter.class, BlockPos.class);
		pipe.getDeclaredMethod("getBlockSupportShape", BlockState.class, BlockGetter.class, BlockPos.class);
		pipe.getDeclaredMethod("setExtracting", Level.class, BlockPos.class, Direction.class, boolean.class);
	}

	@Test
	void pipeTileEntityHasHasReasonToStay() throws Exception {
		Class<?> pipe = pipez("de.maxhenkel.pipez.blocks.tileentity.PipeTileEntity");
		assertEquals(boolean.class, pipe.getDeclaredMethod("hasReasonToStay").getReturnType());
	}

	@Test
	void pipeRendererHasItsRenderMethod() throws Exception {
		Class<?> renderer, tile, poseStack, buffers;
		try {
			renderer = pipez("de.maxhenkel.pipez.blocks.tileentity.render.PipeRenderer");
			tile = pipez("de.maxhenkel.pipez.blocks.tileentity.PipeTileEntity");
			poseStack = Class.forName("com.mojang.blaze3d.vertex.PoseStack", false, loader());
			buffers = Class.forName("net.minecraft.client.renderer.MultiBufferSource", false, loader());
		} catch (LinkageError | ClassNotFoundException | RuntimeException e) {
			// e.g. a server-side test run, where client classes can't be loaded
			Assumptions.abort("client classes aren't available here: " + e);
			return;
		}
		renderer.getDeclaredMethod("render", tile, float.class, poseStack, buffers, int.class, int.class);
	}

	private static List<String> names(String key) {
		JsonElement element = config.get(key);
		List<String> names = new ArrayList<>();
		if (element == null)
			return names;
		JsonArray array = element.getAsJsonArray();
		for (JsonElement e : array)
			names.add(e.getAsString());
		return names;
	}

	private static Class<?> pipez(String name) {
		try {
			return Class.forName(name, false, loader());
		} catch (ClassNotFoundException e) {
			Assumptions.abort("Pipez isn't on the classpath");
			throw new AssertionError(e);
		}
	}
}
