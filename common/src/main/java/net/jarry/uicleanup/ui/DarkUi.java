package net.jarry.uicleanup.ui;

import net.jarry.uicleanup.config.UiConfig;
import net.minecraft.client.Minecraft;

/**
 * The optional built-in "Jarry's Dark UI" pack. Resource packs cannot change the colour code uses for container
 * labels ("Inventory", "Crafting", ...), which vanilla draws in dark grey. While the pack is enabled (and the
 * matching toggle is on) those label colours are lifted to a light grey so they stay readable on charcoal.
 */
public final class DarkUi {
	public static final String PACK_PATH = "dark_ui";
	public static final int LIGHT_LABEL = 0xFFD8D8DE;
	private static boolean inLabels;
	private static boolean labelsActive;

	private DarkUi() { }

	public static boolean isPackId(String id) {
		return id.contains("jarrys_ui_cleanup") && id.endsWith(PACK_PATH);
	}

	public static boolean packEnabled() {
		Minecraft mc = Minecraft.getInstance();
		if (mc == null) return false;
		for (String id : mc.getResourcePackRepository().getSelectedIds()) if (isPackId(id)) return true;
		return false;
	}

	public static void beginLabels() {
		inLabels = true;
		labelsActive = UiConfig.get().darkUiReadableText && packEnabled();
	}

	public static void endLabels() {
		inLabels = false;
	}

	public static int toastTitleColor(int color) {
		return UiConfig.get().darkUiReadableText && packEnabled() ? remap(color) : color;
	}

	public static int labelColor(int color) {
		return inLabels && labelsActive ? remap(color) : color;
	}

	/** Vanilla's dark purple toast title (recipe and tutorial toasts). */
	public static final int TOAST_PURPLE = 0xFF500050;
	public static final int LIGHT_CRIMSON_TEXT = 0xFFF0686C;

	/** Dark, unsaturated, opaque text colours become light grey; anything coloured or already light is left alone. */
	public static int remap(int argb) {
		if (argb == TOAST_PURPLE) return LIGHT_CRIMSON_TEXT;
		int a = argb >>> 24, r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
		if (a < 0x80) return argb;
		int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
		if (max - min > 24 || max > 0x80) return argb;
		return (a << 24) | (LIGHT_LABEL & 0xFFFFFF);
	}
}
