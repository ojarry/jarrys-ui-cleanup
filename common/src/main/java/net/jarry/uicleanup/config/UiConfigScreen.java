package net.jarry.uicleanup.config;

import java.util.function.BiConsumer;
import java.util.function.Function;
import net.jarry.uicleanup.UiCleanup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

/** Settings screen (Mod Menu on Fabric, the Mods list "Config" button on NeoForge). Every change is saved at once. */
public final class UiConfigScreen extends OptionsSubScreen {
	public UiConfigScreen(Screen parent) {
		super(parent, Minecraft.getInstance().options, Component.translatable("jarrys_ui_cleanup.config.title"));
	}

	private OptionInstance<Boolean> toggle(String key, Function<UiConfig, Boolean> get, BiConsumer<UiConfig, Boolean> set) {
		String base = "jarrys_ui_cleanup.option." + key;
		return OptionInstance.createBoolean(base, OptionInstance.cachedConstantTooltip(Component.translatable(base + ".tooltip")),
			get.apply(UiConfig.get()), v -> { set.accept(UiConfig.get(), v); UiConfig.get().save(); });
	}

	@Override
	protected void addOptions() {
		if (this.list == null) return;
		this.list.addHeader(Component.translatable("jarrys_ui_cleanup.config.menus"));
		this.list.addSmall(
			toggle("removeRealms", c -> c.removeRealms, (c, v) -> c.removeRealms = v),
			toggle("removeFeedbackButtons", c -> c.removeFeedbackButtons, (c, v) -> c.removeFeedbackButtons = v),
			toggle("removeAdvancementsButton", c -> c.removeAdvancementsButton, (c, v) -> c.removeAdvancementsButton = v),
			toggle("moveTitleIcons", c -> c.moveTitleIcons, (c, v) -> c.moveTitleIcons = v),
			toggle("removeTitleAccessibilityLanguage", c -> c.removeTitleAccessibilityLanguage, (c, v) -> c.removeTitleAccessibilityLanguage = v),
			toggle("removeChatReport", c -> c.removeChatReport, (c, v) -> c.removeChatReport = v),
			toggle("stripChatSignatures", c -> c.stripChatSignatures, (c, v) -> c.stripChatSignatures = v),
			toggle("allowSignedChatOnRequiredServers", c -> c.allowSignedChatOnRequiredServers, (c, v) -> c.allowSignedChatOnRequiredServers = v),
			toggle("hideRecipeBookButton", c -> c.hideRecipeBookButton, (c, v) -> c.hideRecipeBookButton = v));
		this.list.addHeader(Component.translatable("jarrys_ui_cleanup.config.popups"));
		this.list.addSmall(
			toggle("skipWorldWarnings", c -> c.skipWorldWarnings, (c, v) -> c.skipWorldWarnings = v),
			toggle("skipAccessibilityOnboarding", c -> c.skipAccessibilityOnboarding, (c, v) -> c.skipAccessibilityOnboarding = v),
			toggle("disableNarratorHotkey", c -> c.disableNarratorHotkey, (c, v) -> c.disableNarratorHotkey = v),
			toggle("hideTutorialToasts", c -> c.hideTutorialToasts, (c, v) -> c.hideTutorialToasts = v),
			toggle("hideChatSecurityToast", c -> c.hideChatSecurityToast, (c, v) -> c.hideChatSecurityToast = v));
		this.list.addHeader(Component.translatable("jarrys_ui_cleanup.config.loading"));
		this.list.addSmall(
			toggle("customLoadingScreen", c -> c.customLoadingScreen, (c, v) -> c.customLoadingScreen = v),
			toggle("fasterLoading", c -> c.fasterLoading, (c, v) -> c.fasterLoading = v),
			toggle("disableTelemetry", c -> c.disableTelemetry, (c, v) -> c.disableTelemetry = v),
			toggle("darkUiReadableText", c -> c.darkUiReadableText, (c, v) -> c.darkUiReadableText = v));
		if (UiCleanup.noChatReportsPresent()) {
			UiCleanup.LOG.debug("No Chat Reports present; the chat-report toggle has no effect");
		}
	}
}
