package com.comedymonkey.createaddons.compat.veil;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

/**
 * Makes Flywheel's instances write Veil's albedo and normal buffers, as vanilla's shaders do once Veil has rewritten
 * them. Veil's coloured light (the Electric Lights' glow, {@link LightGlow}) lights each pixel with the surface colour
 * and normal it finds in those buffers. Flywheel draws its instances while they're bound but its own fragment shader
 * never writes them, so under a coloured light an instanced part (a machine's moving parts, the Helper Bot, a chest)
 * was lit with whatever had been drawn behind it, and that showed through it.
 * <p>
 * Flywheel's common fragment shader is changed as Flywheel loads it, only where the coloured light can run (the same
 * test as {@link LightGlow}'s install): its colour output takes location 0, and two more outputs carry the colour before
 * lighting (texture times vertex colour, as Veil's albedo) at location 1 and the view-space normal at location 2. Those
 * are the places Veil gives albedo and normal while both are on, which the coloured light holds them while any lamp
 * glows. Where nothing is bound there, the writes go nowhere. If Flywheel's shader isn't the text this expects (a
 * newer Flywheel), it's left alone and that's logged once.
 */
public final class VeilFlywheelOutputs {
	private static final Logger LOGGER = LogUtils.getLogger();

	/** Flywheel's common fragment shader, as its loader names it. */
	static final String COMMON_FRAG = "flywheel:internal/common.frag";

	static final String OUTPUT = "out vec4 _flw_outputColor;";
	static final String BEFORE_LIGHTING = "vec4 color = flw_fragColor;";
	static final String WRITE = "_flw_outputColor = color;";

	private static boolean warned;

	/** Veil's buffers are bound for Flywheel's draw this frame ({@link LightGlow}); put back by {@link #afterFlywheel}. */
	static boolean boundForFlywheel;
	/** Unbinds them: set by {@link LightGlow} when it installs (this class names no Veil type, so it loads without Veil). */
	static Runnable unbind;

	private VeilFlywheelOutputs() {}

	/** Flywheel has drawn its instances (LevelRendererMixin): the screen goes back to how Flywheel found it. */
	public static void afterFlywheel() {
		if (!boundForFlywheel)
			return;
		boundForFlywheel = false;
		if (unbind != null)
			unbind.run();
	}

	/** Whether the Electric Lights' coloured light runs through Veil here: Veil, and neither Iris nor Colorful Lighting. */
	public static boolean colouredLight() {
		var mods = net.neoforged.fml.ModList.get();
		return mods != null && mods.isLoaded("veil") && !mods.isLoaded("iris") && !mods.isLoaded("colorful_lighting");
	}

	/** Called for every Flywheel shader file as it loads: its source, changed if it's the common fragment shader. */
	public static String onLoad(String name, String source) {
		if (!COMMON_FRAG.equals(name) || !colouredLight())
			return source;
		String patched = patch(source);
		if (patched != source)
			LOGGER.info("Flywheel's {} now writes Veil's albedo and normal, for the Electric Lights' coloured light", COMMON_FRAG);
		else if (!warned) {
			warned = true;
			LOGGER.warn("Flywheel's {} isn't the shader this was written for: instanced parts won't take the Electric Lights'"
				+ " coloured light properly (they may show what's behind them)", COMMON_FRAG);
		}
		return patched;
	}

	/** The common fragment shader writing Veil's albedo and normal too; the same text if it isn't the shader expected. */
	static String patch(String source) {
		if (count(source, OUTPUT) != 1 || count(source, BEFORE_LIGHTING) != 1 || count(source, WRITE) != 1 || source.contains("cmca_veil"))
			return source;
		return source
			.replace(OUTPUT, "layout(location = 0) out vec4 _flw_outputColor;\n"
				+ "// ComedyMonkey's Create Addons: Veil's albedo and normal, for its coloured light\n"
				+ "layout(location = 1) out vec4 cmca_veilAlbedo;\n"
				+ "layout(location = 2) out vec4 cmca_veilNormal;")
			.replace(BEFORE_LIGHTING, BEFORE_LIGHTING + "\n    vec4 cmca_albedo = color;")
			.replace(WRITE, WRITE + "\n"
				+ "    #ifndef _FLW_CRUMBLING\n"
				+ "    cmca_veilAlbedo = cmca_albedo;\n"
				+ "    cmca_veilNormal = vec4(normalize(mat3(flw_view) * flw_vertexNormal), 1.0);\n"
				+ "    #endif");
	}

	private static int count(String text, String part) {
		int n = 0;
		for (int i = text.indexOf(part); i >= 0; i = text.indexOf(part, i + part.length()))
			n++;
		return n;
	}
}
