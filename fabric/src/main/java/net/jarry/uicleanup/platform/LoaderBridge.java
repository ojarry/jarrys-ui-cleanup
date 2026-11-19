package net.jarry.uicleanup.platform;

import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Fabric side of the loader bridge. */
public final class LoaderBridge {
	private LoaderBridge() { }

	public static boolean isModLoaded(String id) { return FabricLoader.getInstance().isModLoaded(id); }
	public static Path configDir() { return FabricLoader.getInstance().getConfigDir(); }
	public static String loaderName() { return "Fabric"; }

	/** Lines of version text in the title screen's bottom-left corner (Mod Menu may add a mod-count line). */
	public static int titleBrandingLines() { return isModLoaded("modmenu") ? 2 : 1; }
}
