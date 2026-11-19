package net.jarry.uicleanup.platform;

import java.nio.file.Path;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

/** NeoForge side of the loader bridge. Uses the loading mod list so it also works while mixins are being set up. */
public final class LoaderBridge {
	private LoaderBridge() { }

	public static boolean isModLoaded(String id) {
		FMLLoader loader = FMLLoader.getCurrentOrNull();
		return loader != null && loader.getLoadingModList().getModFileById(id) != null;
	}

	public static Path configDir() { return FMLPaths.CONFIGDIR.get(); }
	public static String loaderName() { return "NeoForge"; }

	/** Lines of version text NeoForge draws in the title screen's bottom-left corner. */
	public static int titleBrandingLines() {
		int[] n = {0};
		net.neoforged.neoforge.internal.BrandingControl.forEachLine(true, true, (i, s) -> n[0]++);
		return Math.max(1, n[0]);
	}
}
