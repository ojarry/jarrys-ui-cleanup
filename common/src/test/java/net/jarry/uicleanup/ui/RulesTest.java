package net.jarry.uicleanup.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RulesTest {
	@Test
	void removalKeysFollowToggles() {
		assertTrue(RemovalRules.keys(true, true, true).containsAll(java.util.List.of("menu.online", "menu.sendFeedback", "menu.reportBugs", "menu.playerReporting")));
		assertTrue(RemovalRules.keys(false, false, false).isEmpty());
		assertFalse(RemovalRules.keys(true, false, false).contains("menu.reportBugs"));
		assertFalse(RemovalRules.keys(true, true, false).contains("menu.playerReporting"), "chat report off (or No Chat Reports present) keeps that button");
		assertFalse(RemovalRules.keys(true, true, true).contains("menu.multiplayer"));
	}

	@Test
	void darkLabelRemap() {
		assertEquals(DarkUi.LIGHT_LABEL, DarkUi.remap(0xFF404040), "vanilla container label grey");
		assertEquals(DarkUi.LIGHT_LABEL, DarkUi.remap(0xFF000000));
		assertEquals(0xFFFFFFFF, DarkUi.remap(0xFFFFFFFF), "white stays");
		assertEquals(0xFF80FF20, DarkUi.remap(0xFF80FF20), "coloured (e.g. XP green) stays");
		assertEquals(0xFF8B0000, DarkUi.remap(0xFF8B0000), "dark red is coloured, stays");
		assertEquals(0x00404040, DarkUi.remap(0x00404040), "transparent stays");
		assertEquals(DarkUi.labelColor(0xFF404040), 0xFF404040, "outside label drawing nothing changes");
	}

	@Test
	void packIdMatching() {
		assertTrue(DarkUi.isPackId("jarrys_ui_cleanup:dark_ui"));
		assertTrue(DarkUi.isPackId("mod/jarrys_ui_cleanup:resourcepacks/dark_ui"));
		assertFalse(DarkUi.isPackId("vanilla"));
		assertFalse(DarkUi.isPackId("file/dark_ui"));
	}
}
