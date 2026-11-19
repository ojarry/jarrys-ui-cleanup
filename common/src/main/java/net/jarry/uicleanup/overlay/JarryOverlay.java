package net.jarry.uicleanup.overlay;

import net.jarry.uicleanup.UiCleanup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/** Draws the near-black crimson loading screen: background, soft crimson glow and vignette, Jarry logo, progress bar. */
public final class JarryOverlay {
	public static final int BASE = 0xFF0B0405;
	public static final int CRIMSON = 0xFFBC080C;
	private static final int BAR_TRACK = 0xFF1C0809, BAR_EDGE = 0xFF5E0407;
	static final Identifier LOGO = Identifier.fromNamespaceAndPath(UiCleanup.MOD_ID, "textures/gui/loading_logo.png");
	static final Identifier GLOW = Identifier.fromNamespaceAndPath(UiCleanup.MOD_ID, "textures/gui/loading_glow.png");
	private static boolean registered, failed;
	public static volatile int framesDrawn;

	private JarryOverlay() { }

	private static boolean ensureTextures() {
		if (registered) return !failed;
		registered = true;
		try {
			var tm = Minecraft.getInstance().getTextureManager();
			tm.registerAndLoad(LOGO, new ClasspathTexture(LOGO, "/assets/jarrys_ui_cleanup/textures/gui/loading_logo.png"));
			tm.registerAndLoad(GLOW, new ClasspathTexture(GLOW, "/assets/jarrys_ui_cleanup/textures/gui/loading_glow.png"));
		} catch (RuntimeException e) {
			failed = true;
			UiCleanup.LOG.warn("Loading-screen logo could not be loaded; showing the plain background", e);
		}
		return !failed;
	}

	/** Logo box size for a GUI of the given size; pure, unit-tested. */
	public static int logoSize(int guiW, int guiH) {
		return Math.max(48, (int) (Math.min(guiW * 0.75, guiH) * 0.42));
	}

	public static int withAlpha(int rgb, float alpha) {
		return ARGB.color(Mth.clamp(Math.round(alpha * 255.0F), 0, 255), rgb & 0xFFFFFF);
	}

	/**
	 * @param bgAlpha     opacity of the full-screen background (1 = opaque)
	 * @param contentAlpha opacity of the logo, glow and bar
	 */
	public static void draw(GuiGraphicsExtractor g, float bgAlpha, float contentAlpha, float progress, boolean showBar) {
		framesDrawn++;
		int w = g.guiWidth(), h = g.guiHeight();
		g.fill(0, 0, w, h, withAlpha(BASE, bgAlpha));
		// vignette: slightly darker towards top and bottom edges
		int band = h / 5;
		g.fillGradient(0, 0, w, band, withAlpha(0x000000, 0.55F * bgAlpha), withAlpha(0x000000, 0));
		g.fillGradient(0, h - band, w, h, withAlpha(0x000000, 0), withAlpha(0x000000, 0.55F * bgAlpha));
		int size = logoSize(w, h);
		int cx = w / 2, cy = (int) (h * 0.45);
		boolean tex = ensureTextures();
		int white = ARGB.white(contentAlpha);
		if (tex) {
			int gs = (int) (size * 1.9);
			g.blit(RenderPipelines.GUI_TEXTURED, GLOW, cx - gs / 2, cy - gs / 2, 0, 0, gs, gs, gs, gs, white);
			g.blit(RenderPipelines.GUI_TEXTURED, LOGO, cx - size / 2, cy - size / 2, 0, 0, size, size, size, size, white);
		}
		if (showBar) {
			int barW = Math.min(w - 40, size * 2);
			int x0 = cx - barW / 2, x1 = cx + barW / 2;
			int y0 = Math.min(h - 14, cy + size / 2 + 14), y1 = y0 + 6;
			g.fill(x0, y0, x1, y1, withAlpha(BAR_EDGE, contentAlpha));
			g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, withAlpha(BAR_TRACK, contentAlpha));
			int fill = Mth.ceil((barW - 2) * Mth.clamp(progress, 0.0F, 1.0F));
			if (fill > 0) g.fill(x0 + 1, y0 + 1, x0 + 1 + fill, y1 - 1, withAlpha(CRIMSON, contentAlpha));
		}
	}
}
