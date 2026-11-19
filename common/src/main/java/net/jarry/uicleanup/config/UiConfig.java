package net.jarry.uicleanup.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.jarry.uicleanup.UiCleanup;
import net.jarry.uicleanup.platform.LoaderBridge;

/**
 * Plain JSON settings in config/jarrys_ui_cleanup.json. Missing keys keep their defaults, unknown keys
 * are ignored, and a broken file is left untouched (the defaults are used and a warning is logged).
 */
public final class UiConfig {
	public static final String FILE_NAME = UiCleanup.MOD_ID + ".json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static UiConfig instance;
	private static Path pathOverride;

	public boolean removeRealms = true;
	public boolean removeFeedbackButtons = true;
	public boolean removeAdvancementsButton = true;
	public boolean moveTitleIcons = true;
	/** Remove the Accessibility and Language icons from the title screen (both remain in Options). */
	public boolean removeTitleAccessibilityLanguage = true;
	public boolean skipWorldWarnings = true;
	public boolean removeChatReport = true;
	/** Don't sign outgoing chat/commands. Ignored when No Chat Reports is installed. */
	public boolean stripChatSignatures = true;
	/** On servers that enforce secure chat, sign normally there (otherwise chat is blocked there with a notice). */
	public boolean allowSignedChatOnRequiredServers = true;
	public boolean customLoadingScreen = true;
	public boolean skipAccessibilityOnboarding = true;
	public boolean disableNarratorHotkey = true;
	public boolean hideTutorialToasts = true;
	public boolean hideChatSecurityToast = true;
	public boolean fasterLoading = true;
	public boolean hideRecipeBookButton = false;
	public boolean disableTelemetry = true;
	public boolean darkUiReadableText = true;

	public static synchronized UiConfig get() {
		if (instance == null) instance = load(path());
		return instance;
	}

	/** Tests only. */
	public static synchronized void useFile(Path p) {
		pathOverride = p;
		instance = null;
	}

	public static Path path() {
		return pathOverride != null ? pathOverride : LoaderBridge.configDir().resolve(FILE_NAME);
	}

	static UiConfig load(Path p) {
		if (Files.isRegularFile(p)) {
			try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
				UiConfig c = GSON.fromJson(r, UiConfig.class);
				if (c != null) return c;
			} catch (IOException | RuntimeException e) {
				UiCleanup.LOG.warn("Could not read {}, using defaults (file left as is)", p, e);
				return new UiConfig();
			}
		}
		UiConfig c = new UiConfig();
		c.saveTo(p);
		return c;
	}

	public synchronized void save() {
		saveTo(path());
	}

	private void saveTo(Path p) {
		try {
			Files.createDirectories(p.getParent());
			Path tmp = p.resolveSibling(p.getFileName() + ".tmp");
			try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
				GSON.toJson(this, w);
			}
			Files.move(tmp, p, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException | RuntimeException e) {
			UiCleanup.LOG.warn("Could not save {}", p, e);
		}
	}

	public String toJson() {
		return GSON.toJson(this);
	}

	public static UiConfig fromJson(String json) {
		UiConfig c = GSON.fromJson(json, UiConfig.class);
		return c == null ? new UiConfig() : c;
	}
}
