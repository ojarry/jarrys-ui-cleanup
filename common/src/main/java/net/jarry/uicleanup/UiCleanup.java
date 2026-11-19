package net.jarry.uicleanup;

import net.jarry.uicleanup.config.UiConfig;
import net.jarry.uicleanup.platform.LoaderBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared, loader-independent entry point. Everything in this mod is client-side and never touches world data. */
public final class UiCleanup {
	public static final String MOD_ID = "jarrys_ui_cleanup";
	public static final String NAME = "Jarry's UI Cleanup";
	public static final String NO_CHAT_REPORTS = "nochatreports";
	public static final Logger LOG = LoggerFactory.getLogger(NAME);

	private static Boolean ncr;
	/** Counters for the self-test and the log; never saved anywhere. */
	public static int skippedCreateWarnings, skippedBackupPrompts;

	private UiCleanup() { }

	public static void init() {
		UiConfig.get();
		LOG.info("{} loaded on {} (No Chat Reports present: {}, chat-report removal and signature stripping {})", NAME, LoaderBridge.loaderName(),
			noChatReportsPresent(), noChatReportsPresent() ? "stepping aside" : "active");
	}

	/** True when No Chat Reports is installed; then our chat-report feature does nothing and its mixin is not applied. */
	public static boolean noChatReportsPresent() {
		if (ncr == null) ncr = LoaderBridge.isModLoaded(NO_CHAT_REPORTS);
		return ncr;
	}

	public static boolean chatReportRemovalActive() {
		return UiConfig.get().removeChatReport && !noChatReportsPresent();
	}
}
