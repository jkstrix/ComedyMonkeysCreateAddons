package com.comedymonkey.createaddons.client.line;

import java.util.OptionalDouble;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderType;

/**
 * Lines drawn through everything in front of them: the goggles' outline of a hidden line, whole, through the block it's
 * encased in (vanilla's lines are depth-tested, so only the bits flush with the casing's faces showed, as stray squares).
 * Vanilla's NO_DEPTH_TEST only leaves the depth test as it finds it, and what renders before this stage can leave it on
 * (behind Minecraft's back, too), so the test is switched off in GL itself and put back exactly as it was.
 */
final class XrayLines extends RenderType {
	private static boolean depthWasOn;

	private static final LayeringStateShard THROUGH_EVERYTHING = new LayeringStateShard("comedymonkeysaddons_through_everything", () -> {
		depthWasOn = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
	}, () -> {
		if (depthWasOn)
			GL11.glEnable(GL11.GL_DEPTH_TEST);
	});

	static final RenderType TYPE = create("comedymonkeysaddons_xray_lines", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES,
		1536, false, false, CompositeState.builder()
			.setShaderState(RENDERTYPE_LINES_SHADER)
			.setLineState(new LineStateShard(OptionalDouble.empty()))
			.setLayeringState(THROUGH_EVERYTHING)
			.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
			.setOutputState(MAIN_TARGET)
			.setWriteMaskState(COLOR_WRITE)
			.setCullState(NO_CULL)
			.setDepthTestState(NO_DEPTH_TEST)
			.createCompositeState(false));

	private XrayLines(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload,
		Runnable setup, Runnable clear) {
		super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
	}
}
