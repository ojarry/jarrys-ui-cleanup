package net.jarry.uicleanup.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UiConfigTest {
	@Test
	void defaults() {
		UiConfig c = new UiConfig();
		assertTrue(c.removeRealms && c.removeFeedbackButtons && c.removeAdvancementsButton && c.moveTitleIcons && c.removeTitleAccessibilityLanguage && c.stripChatSignatures && c.allowSignedChatOnRequiredServers && c.skipWorldWarnings && c.removeChatReport && c.customLoadingScreen
			&& c.skipAccessibilityOnboarding && c.disableNarratorHotkey && c.hideTutorialToasts && c.hideChatSecurityToast
			&& c.fasterLoading && c.disableTelemetry && c.darkUiReadableText, "everything on by default");
		assertFalse(c.hideRecipeBookButton, "recipe book hiding is opt-in");
	}

	@Test
	void missingFileIsCreatedWithDefaults(@TempDir Path dir) {
		Path p = dir.resolve("sub/jarrys_ui_cleanup.json");
		UiConfig c = UiConfig.load(p);
		assertTrue(Files.exists(p));
		assertTrue(c.removeRealms);
	}

	@Test
	void partialFileKeepsOtherDefaults(@TempDir Path dir) throws Exception {
		Path p = dir.resolve("c.json");
		Files.writeString(p, "{\"removeRealms\": false, \"hideRecipeBookButton\": true, \"unknownKey\": 5}");
		UiConfig c = UiConfig.load(p);
		assertFalse(c.removeRealms);
		assertTrue(c.hideRecipeBookButton);
		assertTrue(c.fasterLoading);
	}

	@Test
	void brokenFileIsNotOverwritten(@TempDir Path dir) throws Exception {
		Path p = dir.resolve("c.json");
		Files.writeString(p, "{ this is not json");
		UiConfig c = UiConfig.load(p);
		assertTrue(c.removeRealms);
		assertEquals("{ this is not json", Files.readString(p));
	}

	@Test
	void saveRoundTrip(@TempDir Path dir) {
		UiConfig.useFile(dir.resolve("rt.json"));
		UiConfig.get().disableTelemetry = false;
		UiConfig.get().save();
		UiConfig.useFile(dir.resolve("rt.json"));
		assertFalse(UiConfig.get().disableTelemetry);
		assertEquals(UiConfig.get().toJson(), UiConfig.fromJson(UiConfig.get().toJson()).toJson());
	}
}
