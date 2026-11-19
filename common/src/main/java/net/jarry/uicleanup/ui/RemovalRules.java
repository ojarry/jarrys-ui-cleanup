package net.jarry.uicleanup.ui;

import java.util.HashSet;
import java.util.Set;

/** Which vanilla button labels (translation keys) are removed for a given set of toggles. Pure logic, unit-tested. */
public final class RemovalRules {
	public static final String REALMS = "menu.online";
	public static final Set<String> FEEDBACK = Set.of("menu.sendFeedback", "menu.reportBugs", "telemetry_info.button.give_feedback");
	public static final String PLAYER_REPORTING = "menu.playerReporting";
	/** Pause menu only. */
	public static final String ADVANCEMENTS = "gui.advancements";
	public static final String STATISTICS = "gui.stats";
	public static final String FRIENDS = "gui.friends.open";
	/** Title screen only (both are still in Options). */
	public static final Set<String> TITLE_ACCESS_LANGUAGE = Set.of("options.accessibility", "options.language");

	private RemovalRules() { }

	public static Set<String> keys(boolean realms, boolean feedback, boolean chatReport) {
		Set<String> s = new HashSet<>();
		if (realms) s.add(REALMS);
		if (feedback) s.addAll(FEEDBACK);
		if (chatReport) s.add(PLAYER_REPORTING);
		return s;
	}
}
