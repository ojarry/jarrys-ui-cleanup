package net.jarry.uicleanup.neoforge;

import net.jarry.uicleanup.UiCleanup;
import net.jarry.uicleanup.config.UiConfigScreen;
import net.jarry.uicleanup.selftest.ClientSelfTest;
import net.jarry.uicleanup.ui.DarkUi;
import net.jarry.uicleanup.ui.ScreenCleaner;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;

@Mod(value = UiCleanup.MOD_ID, dist = Dist.CLIENT)
public final class UiCleanupNeoForge {
	public UiCleanupNeoForge(IEventBus modBus, ModContainer container) {
		UiCleanup.init();
		container.registerExtensionPoint(IConfigScreenFactory.class, (c, parent) -> new UiConfigScreen(parent));
		modBus.addListener(UiCleanupNeoForge::addPacks);
		NeoForge.EVENT_BUS.addListener(UiCleanupNeoForge::afterInit);
		if (ClientSelfTest.enabled()) NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> ClientSelfTest.tick(Minecraft.getInstance()));
	}

	private static void afterInit(ScreenEvent.Init.Post event) {
		ScreenCleaner.clean(event.getScreen(), java.util.List.copyOf(event.getListenersList()), event::removeListener);
	}

	private static void addPacks(AddPackFindersEvent event) {
		if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
		event.addPackFinders(Identifier.fromNamespaceAndPath(UiCleanup.MOD_ID, "resourcepacks/" + DarkUi.PACK_PATH), PackType.CLIENT_RESOURCES,
			Component.translatable("jarrys_ui_cleanup.pack.dark_ui"), PackSource.BUILT_IN, false, Pack.Position.TOP);
	}
}
