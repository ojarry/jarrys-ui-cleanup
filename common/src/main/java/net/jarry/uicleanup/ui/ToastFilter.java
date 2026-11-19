package net.jarry.uicleanup.ui;

import net.jarry.uicleanup.config.UiConfig;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.TutorialToast;

public final class ToastFilter {
	private ToastFilter() { }

	public static boolean blocks(Toast toast) {
		UiConfig cfg = UiConfig.get();
		if (cfg.hideTutorialToasts && toast instanceof TutorialToast) return true;
		return cfg.hideChatSecurityToast && toast instanceof SystemToast s
			&& s.getToken() == SystemToast.SystemToastId.UNSECURE_SERVER_WARNING;
	}
}
