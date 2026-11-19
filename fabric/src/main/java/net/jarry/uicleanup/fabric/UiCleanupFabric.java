package net.jarry.uicleanup.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.jarry.uicleanup.UiCleanup;
import net.jarry.uicleanup.selftest.ClientSelfTest;
import net.jarry.uicleanup.ui.DarkUi;
import net.jarry.uicleanup.ui.ScreenCleaner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class UiCleanupFabric implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		UiCleanup.init();
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			var widgets = Screens.getWidgets(screen);
			ScreenCleaner.clean(screen, widgets, l -> widgets.remove(l));
		});
		var container = FabricLoader.getInstance().getModContainer(UiCleanup.MOD_ID).orElseThrow();
		ResourceLoader.registerBuiltinPack(Identifier.fromNamespaceAndPath(UiCleanup.MOD_ID, DarkUi.PACK_PATH), container,
			Component.translatable("jarrys_ui_cleanup.pack.dark_ui"), PackActivationType.NORMAL);
		if (ClientSelfTest.enabled()) ClientTickEvents.END_CLIENT_TICK.register(ClientSelfTest::tick);
	}
}
