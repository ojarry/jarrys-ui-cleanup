package net.jarry.uicleanup.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.jarry.uicleanup.config.UiConfigScreen;

/** Only loaded by Mod Menu when it is installed (optional dependency). */
public final class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return UiConfigScreen::new;
	}
}
