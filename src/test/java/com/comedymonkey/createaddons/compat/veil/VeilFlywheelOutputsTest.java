package com.comedymonkey.createaddons.compat.veil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/** Flywheel's common fragment shader (1.0.6, as bundled with Create 6.0.10) given Veil's albedo and normal outputs. */
class VeilFlywheelOutputsTest {
	private static String flywheelCommonFrag() throws IOException {
		try (InputStream in = VeilFlywheelOutputsTest.class.getResourceAsStream("/fixtures/flywheel-1.0.6-common.frag")) {
			assertNotNull(in, "fixture missing");
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static int count(String text, String part) {
		int n = 0;
		for (int i = text.indexOf(part); i >= 0; i = text.indexOf(part, i + part.length()))
			n++;
		return n;
	}

	@Test
	void theColourOutputKeepsLocationZeroAndTheTwoBuffersFollow() throws IOException {
		String patched = VeilFlywheelOutputs.patch(flywheelCommonFrag());
		assertEquals(1, count(patched, "layout(location = 0) out vec4 _flw_outputColor;"));
		assertEquals(1, count(patched, "layout(location = 1) out vec4 cmca_veilAlbedo;"));
		assertEquals(1, count(patched, "layout(location = 2) out vec4 cmca_veilNormal;"));
		// the unpatched declaration is gone (only the located one is left)
		assertEquals(1, count(patched, "out vec4 _flw_outputColor;"));
	}

	@Test
	void albedoIsTheColourBeforeLightingAndBothAreWrittenWithTheColour() throws IOException {
		String patched = VeilFlywheelOutputs.patch(flywheelCommonFrag());
		int capture = patched.indexOf("vec4 cmca_albedo = color;");
		int diffuse = patched.indexOf("color.rgb *= diffuseFactor;");
		int lightmap = patched.indexOf("color *= lightColor;");
		assertTrue(capture > 0 && capture < diffuse && capture < lightmap, "albedo is taken before diffuse and lightmap");
		int write = patched.indexOf("_flw_outputColor = color;");
		int albedo = patched.indexOf("cmca_veilAlbedo = cmca_albedo;");
		int normal = patched.indexOf("cmca_veilNormal = vec4(normalize(mat3(flw_view) * flw_vertexNormal), 1.0);");
		assertTrue(write > 0 && albedo > write && normal > albedo, "written beside the colour output");
		// not in the crumbling pass, which draws after Veil's buffers are done
		assertTrue(patched.lastIndexOf("#ifndef _FLW_CRUMBLING", albedo) > write);
	}

	@Test
	void theTransparencyPassesAreLeftAsTheyWere() throws IOException {
		String original = flywheelCommonFrag();
		String patched = VeilFlywheelOutputs.patch(original);
		// the new outputs sit in the plain (non-OIT) branch, where _flw_outputColor is declared
		int oitEnd = patched.indexOf("out vec4 _flw_accumulate;");
		int albedoDecl = patched.indexOf("cmca_veilAlbedo;");
		assertTrue(albedoDecl > oitEnd, "declared after the OIT outputs, in the #else branch");
		assertEquals(count(original, "_flw_accumulate"), count(patched, "_flw_accumulate"));
		assertEquals(count(original, "_flw_coeffs0"), count(patched, "_flw_coeffs0"));
	}

	@Test
	void anUnexpectedShaderIsLeftAlone() {
		String other = "out vec4 fragColor;\nvoid main() { fragColor = vec4(1.0); }\n";
		assertSame(other, VeilFlywheelOutputs.patch(other));
		String twice = VeilFlywheelOutputs.OUTPUT + "\n" + VeilFlywheelOutputs.OUTPUT + "\n" + VeilFlywheelOutputs.BEFORE_LIGHTING + "\n" + VeilFlywheelOutputs.WRITE;
		assertSame(twice, VeilFlywheelOutputs.patch(twice));
	}

	@Test
	void patchingIsDoneOnce() throws IOException {
		String once = VeilFlywheelOutputs.patch(flywheelCommonFrag());
		assertSame(once, VeilFlywheelOutputs.patch(once));
	}

	@Test
	void otherFilesAreNeverTouched() throws IOException {
		String frag = flywheelCommonFrag();
		assertSame(frag, VeilFlywheelOutputs.onLoad("flywheel:internal/instancing/main.frag", frag));
	}
}
