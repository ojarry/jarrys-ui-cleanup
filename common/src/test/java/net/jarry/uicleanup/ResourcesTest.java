package net.jarry.uicleanup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import net.jarry.uicleanup.config.UiConfig;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class ResourcesTest {
	static final Path ROOT = Path.of(System.getProperty("uic.root", "../.."));
	static final String MC = System.getProperty("uic.mc", "26.3");
	static final Path PACK = ROOT.resolve("generated/" + MC + "/resourcepacks/dark_ui");
	static final Path VANILLA = Path.of(System.getProperty("uic.vanilla", "does-not-exist"));

	static BufferedImage img(String cp) throws Exception {
		try (InputStream in = ResourcesTest.class.getResourceAsStream(cp)) {
			assertNotNull(in, cp);
			return ImageIO.read(in);
		}
	}

	@Test
	void iconAndLogoAreCircleMasked() throws Exception {
		for (String p : List.of("/assets/jarrys_ui_cleanup/icon.png", "/assets/jarrys_ui_cleanup/textures/gui/loading_logo.png")) {
			BufferedImage im = img(p);
			assertEquals(512, im.getWidth(), p);
			assertEquals(512, im.getHeight(), p);
			assertEquals(0, im.getRGB(0, 0) >>> 24, p + " transparent corner");
			assertEquals(0, im.getRGB(511, 511) >>> 24, p + " transparent corner");
			assertEquals(255, im.getRGB(256, 256) >>> 24, p + " opaque centre");
		}
	}

	@Test
	void langCoversEveryToggle() throws Exception {
		JsonObject lang;
		try (InputStream in = ResourcesTest.class.getResourceAsStream("/assets/jarrys_ui_cleanup/lang/en_us.json")) {
			lang = JsonParser.parseString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
		}
		for (var f : UiConfig.class.getFields()) {
			if (f.getType() != boolean.class) continue;
			assertTrue(lang.has("jarrys_ui_cleanup.option." + f.getName()), f.getName());
			assertTrue(lang.has("jarrys_ui_cleanup.option." + f.getName() + ".tooltip"), f.getName());
		}
	}

	@Test
	void mixinConfigListsExistingClasses() throws Exception {
		JsonObject m;
		try (InputStream in = ResourcesTest.class.getResourceAsStream("/jarrys_ui_cleanup.mixins.json")) {
			m = JsonParser.parseString(new String(in.readAllBytes())).getAsJsonObject();
		}
		Path src = ROOT.resolve("common/src/main/java/net/jarry/uicleanup/mixin");
		for (var e : m.getAsJsonArray("client")) assertTrue(Files.exists(src.resolve(e.getAsString() + ".java")), e.getAsString());
	}

	@Test
	void darkPackMatchesThisVersion() throws Exception {
		JsonObject meta = JsonParser.parseString(Files.readString(PACK.resolve("pack.mcmeta"))).getAsJsonObject().getAsJsonObject("pack");
		int expected = MC.equals("26.2") ? 88 : 97;
		assertEquals(expected, meta.get("min_format").getAsInt());
		assertEquals(expected, meta.get("max_format").getAsInt());
		assertTrue(Files.exists(PACK.resolve("pack.png")));
	}

	@Test
	void darkPackKeepsVanillaTextureSizes() throws Exception {
		Assumptions.assumeTrue(Files.isDirectory(VANILLA), "vanilla GUI textures not provided (set uic.vanilla)");
		Path tex = PACK.resolve("assets/minecraft/textures/gui");
		int checked = 0;
		try (Stream<Path> s = Files.walk(tex)) {
			for (Path p : s.filter(x -> x.toString().endsWith(".png")).toList()) {
				Path v = VANILLA.resolve(tex.relativize(p).toString());
				if (!Files.exists(v)) continue;
				BufferedImage a = ImageIO.read(p.toFile()), b = ImageIO.read(v.toFile());
				assertEquals(b.getWidth(), a.getWidth(), p.toString());
				assertEquals(b.getHeight(), a.getHeight(), p.toString());
				checked++;
			}
		}
		assertTrue(checked > 150, "checked " + checked);
	}

	@Test
	void nineSliceMetadataMatchesImages() throws Exception {
		Path spr = PACK.resolve("assets/minecraft/textures/gui/sprites");
		try (Stream<Path> s = Files.walk(spr)) {
			for (Path m : s.filter(x -> x.toString().endsWith(".png.mcmeta")).toList()) {
				JsonObject sc = JsonParser.parseString(Files.readString(m)).getAsJsonObject().getAsJsonObject("gui");
				if (sc == null) continue;
				sc = sc.getAsJsonObject("scaling");
				if (!sc.has("width")) continue;
				BufferedImage im = ImageIO.read(Path.of(m.toString().replace(".mcmeta", "")).toFile());
				assertEquals(sc.get("width").getAsInt(), im.getWidth(), m.toString());
				assertEquals(sc.get("height").getAsInt(), im.getHeight(), m.toString());
			}
		}
	}

	@Test
	void containerLabelsAreDarkEnoughForLightText() throws Exception {
		BufferedImage inv = ImageIO.read(PACK.resolve("assets/minecraft/textures/gui/container/inventory.png").toFile());
		int rgb = inv.getRGB(100, 70); // panel area behind the "Crafting" label
		int lum = ((rgb >> 16 & 255) + (rgb >> 8 & 255) + (rgb & 255)) / 3;
		assertTrue(lum < 70, "panel luminance " + lum);
	}
}
