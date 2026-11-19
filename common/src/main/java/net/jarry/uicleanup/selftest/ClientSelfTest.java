package net.jarry.uicleanup.selftest;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.friends.FriendsOverlayScreen;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;

import com.mojang.serialization.Lifecycle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.jarry.uicleanup.UiCleanup;
import net.jarry.uicleanup.config.UiConfig;
import net.jarry.uicleanup.config.UiConfigScreen;
import net.jarry.uicleanup.overlay.JarryOverlay;
import net.jarry.uicleanup.ui.DarkUi;
import net.jarry.uicleanup.ui.RemovalRules;
import net.jarry.uicleanup.ui.ScreenCleaner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.telemetry.TelemetryEventSender;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.DataPackConfig;

/**
 * Dev-only client self-test (-Djarrys_ui_cleanup.clientselftest=true). Walks the title screen, settings screen,
 * a resource reload, an experimental world being created and re-opened, the pause menu, toasts, the recipe book
 * toggle and the Dark UI pack, asking the harness for a screenshot at each stop, then quits.
 */
public final class ClientSelfTest {
	private static final String WORLD = "uic_experimental";
	private static int step, wait, ticks;
	private static boolean ok = true;
	private static String pendingShot;
	private static long shotAsked;
	private static final List<String> results = new ArrayList<>();
	private static Screen keep;

	private ClientSelfTest() { }

	public static boolean enabled() {
		return Boolean.getBoolean("jarrys_ui_cleanup.clientselftest");
	}

	static void check(boolean cond, String what) {
		if (!cond) ok = false;
		String line = (cond ? "ok   " : "FAIL ") + what;
		results.add(line);
		UiCleanup.LOG.info("CLIENTTEST {}", line);
	}

	private static Path shotFile() {
		return Path.of("uic-shot.txt").toAbsolutePath();
	}

	private static void shot(String name) {
		try {
			Files.writeString(shotFile(), name);
			pendingShot = name;
			shotAsked = System.currentTimeMillis();
			UiCleanup.LOG.info("CLIENTTEST SHOT {}", name);
		} catch (Exception e) {
			UiCleanup.LOG.warn("shot request failed", e);
		}
	}

	private static AbstractWidget widget(Screen s, String key) {
		for (GuiEventListener l : s.children()) {
			if (l instanceof AbstractWidget w && w.visible && key.equals(ScreenCleaner.key(w.getMessage()))) return w;
		}
		return null;
	}

	private static String overlaps(Screen s) {
		List<AbstractWidget> ws = new ArrayList<>();
		for (GuiEventListener l : s.children()) if (l instanceof AbstractWidget w && w.visible && w.getWidth() > 0) ws.add(w);
		for (int i = 0; i < ws.size(); i++) for (int j = i + 1; j < ws.size(); j++) {
			AbstractWidget a = ws.get(i), b = ws.get(j);
			if (a.getX() < b.getX() + b.getWidth() && b.getX() < a.getX() + a.getWidth() && a.getY() < b.getY() + b.getHeight() && b.getY() < a.getY() + a.getHeight())
				return ScreenCleaner.key(a.getMessage()) + " overlaps " + ScreenCleaner.key(b.getMessage());
		}
		return null;
	}

	private static boolean hasKey(Screen s, String key) {
		for (GuiEventListener l : s.children()) {
			if (l instanceof AbstractWidget w && key.equals(ScreenCleaner.key(w.getMessage()))) return true;
		}
		return false;
	}

	private static String describe(Screen s) {
		StringBuilder b = new StringBuilder();
		for (GuiEventListener l : s.children()) {
			if (l instanceof AbstractWidget w) b.append(String.format("%n   %-28s %s @%d,%d %dx%d", w.getClass().getSimpleName(),
				ScreenCleaner.key(w.getMessage()), w.getX(), w.getY(), w.getWidth(), w.getHeight()));
		}
		return b.toString();
	}

	private static boolean darkSelected(PackRepository repo) {
		return repo.getSelectedIds().stream().anyMatch(DarkUi::isPackId);
	}

	private static void setDark(Minecraft mc, boolean on) {
		PackRepository repo = mc.getResourcePackRepository();
		List<String> ids = new ArrayList<>(repo.getSelectedIds());
		ids.removeIf(DarkUi::isPackId);
		if (on) {
			String id = repo.getAvailableIds().stream().filter(DarkUi::isPackId).findFirst().orElse(null);
			check(id != null, "Dark UI pack is offered in the pack list (available: " + repo.getAvailableIds() + ")");
			if (id != null) ids.add(id);
		}
		repo.setSelected(ids);
		mc.options.updateResourcePacks(repo);
	}

	public static void tick(Minecraft mc) {
		ticks++;
		if (pendingShot != null) {
			if (Files.exists(shotFile()) && System.currentTimeMillis() - shotAsked < 15000) return;
			pendingShot = null;
		}
		if (wait > 0) { wait--; return; }
		Screen screen = mc.gui.screen();
		try {
			switch (step) {
				case 0 -> {
					if (mc.gui.overlay() != null || !(screen instanceof TitleScreen)) return;
					check(JarryOverlay.framesDrawn > 0, "startup loading overlay was drawn by Jarry's overlay (" + JarryOverlay.framesDrawn + " frames)");
					check(!ScreenCleaner.SEEN.stream().anyMatch(n -> n.endsWith("AccessibilityOnboardingScreen")), "accessibility onboarding skipped; screens so far " + ScreenCleaner.SEEN);
					check(!mc.options.onboardAccessibility, "onboarding flag cleared");
					wait = 40; step++;
				}
				case 1 -> {
					UiCleanup.LOG.info("CLIENTTEST title widgets:{}", describe(screen));
					check(!hasKey(screen, RemovalRules.REALMS), "title screen has no Realms button");
					check(hasKey(screen, "menu.multiplayer") && hasKey(screen, "menu.options") && hasKey(screen, "menu.quit"), "title screen keeps Multiplayer, Options and Quit");
					check(!hasKey(screen, "options.accessibility") && !hasKey(screen, "options.language"), "title screen has no Accessibility or Language icon (both still in Options)");
					check(hasKey(screen, RemovalRules.FRIENDS), "title screen keeps the Friends icon");
					List<AbstractWidget> icons = new ArrayList<>();
					for (String k : List.of("options.accessibility", "options.language", RemovalRules.FRIENDS, "fml.menu.mods", "modmenu.title")) {
						AbstractWidget w = widget(screen, k);
						if (w != null) icons.add(w);
					}
					icons.sort(java.util.Comparator.comparingInt(AbstractWidget::getX));
					int lines = net.jarry.uicleanup.platform.LoaderBridge.titleBrandingLines();
					int wantY = screen.height - 10 * lines - 4 - 20;
					boolean corner = icons.size() >= 1;
					for (int i = 0; i < icons.size(); i++) corner &= icons.get(i).getX() == 4 + 24 * i && icons.get(i).getY() == wantY;
					check(corner, icons.size() + " title icons in a row in the bottom-left corner at x=4 step 24, y=" + wantY + " (" + lines + " version line(s) below)");
					AbstractWidget mp = widget(screen, "menu.multiplayer"), op = widget(screen, "menu.options");
					check(mp != null && op != null && op.getY() == mp.getY() + 24, "Options/Quit moved up flush under Multiplayer");
					String tov = overlaps(screen);
					check(tov == null, "no title-screen buttons overlap" + (tov == null ? "" : ": " + tov));
					shot("01-title"); step++;
				}
				case 2 -> {
					UiConfig.get().removeRealms = false;
					TitleScreen plain = new TitleScreen();
					mc.gui.setScreen(plain);
					check(hasKey(plain, RemovalRules.REALMS), "with the Realms toggle off, the Realms button is back (toggle works)");
					UiConfig.get().removeRealms = true;
					UiConfig.get().removeTitleAccessibilityLanguage = false;
					TitleScreen withIcons = new TitleScreen();
					mc.gui.setScreen(withIcons);
					check(hasKey(withIcons, "options.accessibility") && hasKey(withIcons, "options.language"), "with the Accessibility/Language toggle off, both icons are back (toggle works)");
					String wov = overlaps(withIcons);
					check(wov == null, "toggle off: no title-screen buttons overlap" + (wov == null ? "" : ": " + wov));
					UiConfig.get().removeTitleAccessibilityLanguage = true;
					mc.gui.setScreen(screen);
					keep = screen;
					mc.gui.setScreen(new UiConfigScreen(screen));
					wait = 30; step++;
				}
				case 3 -> { shot("02-settings"); step++; }
				case 4 -> {
					mc.gui.setScreen(keep);
					int before = JarryOverlay.framesDrawn;
					mc.reloadResourcePacks();
					check(mc.gui.overlay() instanceof LoadingOverlay, "resource reload shows the loading overlay");
					wait = 2; step++;
					UiCleanup.LOG.info("CLIENTTEST reload started, frames before {}", before);
				}
				case 5 -> { shot("03-loading-overlay"); step++; }
				case 6 -> {
					if (mc.gui.overlay() != null) return;
					// Telemetry and toasts
					check(mc.getTelemetryManager().getOutsideSessionSender() == TelemetryEventSender.DISABLED, "telemetry sender is the disabled sender");
					mc.gui.toastManager().addToast(new TutorialToast(mc.font, TutorialToast.Icons.MOVEMENT_KEYS, Component.literal("tutorial"), null, false));
					SystemToast.add(mc.gui.toastManager(), SystemToast.SystemToastId.UNSECURE_SERVER_WARNING, Component.literal("insecure"), Component.literal("x"));
					SystemToast.add(mc.gui.toastManager(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, Component.literal("control toast"), Component.literal("should show"));
					wait = 10; step++;
				}
				case 7 -> {
					var tm = mc.gui.toastManager();
					check(tm.getToast(SystemToast.class, SystemToast.SystemToastId.PERIODIC_NOTIFICATION) != null, "control toast is shown (toasts still work)");
					check(tm.getToast(SystemToast.class, SystemToast.SystemToastId.UNSECURE_SERVER_WARNING) == null, "chat-security toast was dropped");
					check(tm.getToast(TutorialToast.class, net.minecraft.client.gui.components.toasts.Toast.NO_TOKEN) == null, "tutorial toast was dropped");
					long delay = (long) readField(new LevelLoadTracker(500L), "closeDelayMs");
					check(delay == 0L, "new-world loading screen close delay removed (" + delay + " ms)");
					// Create-world experimental warning
					boolean[] ran = {false};
					WorldOpenFlows.confirmWorldCreation(mc, null, Lifecycle.experimental(), () -> ran[0] = true, false);
					check(ran[0] && !(mc.gui.screen() instanceof net.minecraft.client.gui.screens.ConfirmScreen), "experimental create-world warning skipped (continued straight away)");
					// Create a world with an experimental feature pack so that re-opening it would normally ask for a backup.
					ScreenCleaner.SEEN.clear();
					FeatureFlagSet flags = FeatureFlags.DEFAULT_FLAGS.join(FeatureFlagSet.of(FeatureFlags.TRADE_REBALANCE));
					WorldDataConfiguration data = new WorldDataConfiguration(new DataPackConfig(List.of("vanilla", "trade_rebalance"), List.of()), flags);
					LevelSettings settings = new LevelSettings(WORLD, GameType.SURVIVAL, LevelSettings.DifficultySettings.DEFAULT, true, data);
					mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(20261118L, false, false),
						WorldPresets::createNormalWorldDimensions, new TitleScreen());
					step++;
				}
				case 8 -> {
					if (mc.player == null || mc.level == null || screen != null) return;
					check(true, "test world created and entered; screens: " + ScreenCleaner.SEEN);
					wait = 60; step++;
				}
				case 9 -> { shot("04-in-world"); step = 60; }
				case 60 -> { ChatTest.start(mc); wait = 30; step++; }
				case 61 -> { ChatTest.verifyDelivered(mc); ChatTest.blockedHere(mc); wait = 20; step++; }
				case 62 -> { shot("17-chat-blocked-notice"); step++; }
				case 63 -> { ChatTest.signedHere(mc); wait = 20; step++; }
				case 64 -> { shot("18-chat-signed-here-toast"); step++; }
				case 65 -> { ChatTest.restore(mc); wait = 120; step = 10; }
				case 10 -> { mc.gui.setScreen(new PauseScreen(true)); wait = 30; step++; }
				case 11 -> {
					UiCleanup.LOG.info("CLIENTTEST pause widgets:{}", describe(screen));
					check(!hasKey(screen, "menu.sendFeedback") && !hasKey(screen, "menu.reportBugs"), "pause menu has no Give Feedback / Report Bugs");
					if (UiCleanup.noChatReportsPresent()) check(hasKey(screen, "menu.playerReporting"), "No Chat Reports present: Player Reporting left alone (stepped aside)");
					else check(!hasKey(screen, "menu.playerReporting"), "pause menu has no Player Reporting button");
					check(hasKey(screen, "menu.returnToGame") && hasKey(screen, "menu.options"), "pause menu keeps Back to Game and Options");
					check(!hasKey(screen, RemovalRules.ADVANCEMENTS), "pause menu has no Advancements button");
					AbstractWidget back = widget(screen, "menu.returnToGame"), stats = widget(screen, RemovalRules.STATISTICS),
						friends = widget(screen, RemovalRules.FRIENDS), opts = widget(screen, "menu.options");
					long friendCount = screen.children().stream().filter(l -> l instanceof AbstractWidget w && RemovalRules.FRIENDS.equals(ScreenCleaner.key(w.getMessage()))).count();
					check(back != null && stats != null && friends != null && opts != null && friendCount == 1, "Statistics, one Friends icon, Back to Game and Options are present");
					if (back != null && stats != null && friends != null && opts != null) {
						check(stats.getY() == friends.getY() && stats.getHeight() == friends.getHeight(), "Friends icon sits in the Statistics row");
						int rowRight = stats.getX();
						for (GuiEventListener l : screen.children()) if (l instanceof AbstractWidget w && w.visible && w.getY() == stats.getY()) rowRight = Math.max(rowRight, w.getX() + w.getWidth());
						int rr = rowRight;
						check(stats.getX() == back.getX() && rr == back.getX() + back.getWidth(),
							"Statistics row is flush with the other rows (" + stats.getX() + ".." + rr + " vs " + back.getX() + ".." + (back.getX() + back.getWidth()) + ")");
						int firstIcon = Integer.MAX_VALUE;
						for (GuiEventListener l : screen.children()) if (l instanceof AbstractWidget w && w != stats && w.visible && w.getY() == stats.getY()) firstIcon = Math.min(firstIcon, w.getX());
						check(firstIcon - (stats.getX() + stats.getWidth()) == 8, "8 px gap between Statistics and the icons, like the other paired rows");
						java.util.TreeSet<Integer> rows = new java.util.TreeSet<>();
						for (GuiEventListener l : screen.children()) if (l instanceof AbstractWidget w && w.visible && w.getHeight() == 20 && w.getY() >= back.getY()) rows.add(w.getY());
						boolean even = true; Integer prev = null;
						for (int r : rows) { if (prev != null && r - prev != 24) even = false; prev = r; }
						check(even, "no empty rows left in the pause menu (rows at " + rows + "); other icons from the Friends row (Mods, Player Reporting) stay centred there");
					}
					String ov = overlaps(screen);
					check(ov == null, "no pause-menu buttons overlap" + (ov == null ? "" : ": " + ov));
					shot("05-pause"); step++;
				}
				case 12 -> {
					mc.gui.setScreen(null);
					UiConfig.get().hideRecipeBookButton = true;
					mc.gui.setScreen(new InventoryScreen(mc.player));
					wait = 10; step++;
				}
				case 13 -> {
					check(screen.children().stream().noneMatch(l -> l instanceof ImageButton b && b.getWidth() == 20 && b.getHeight() == 18), "recipe book button hidden when that option is on");
					UiConfig.get().hideRecipeBookButton = false;
					mc.gui.setScreen(null);
					mc.gui.setScreen(new InventoryScreen(mc.player));
					wait = 10; step++;
				}
				case 14 -> {
					check(screen.children().stream().anyMatch(l -> l instanceof ImageButton b && b.getWidth() == 20 && b.getHeight() == 18), "recipe book button back when the option is off (default)");
					shot("06-inventory-default"); step++;
				}
				case 15 -> {
					mc.gui.setScreen(null);
					setDark(mc, true);
					mc.reloadResourcePacks();
					step++;
				}
				case 16 -> {
					if (mc.gui.overlay() != null) return;
					check(darkSelected(mc.getResourcePackRepository()), "Dark UI pack enabled");
					check(DarkUi.packEnabled(), "Dark UI detected as enabled");
					mc.gui.setScreen(new InventoryScreen(mc.player));
					wait = 30; step++;
				}
				case 17 -> { shot("07-dark-inventory"); step++; }
				case 18 -> {
					check(DarkUi.remap(0xFF404040) == DarkUi.LIGHT_LABEL, "dark label colour is lifted to light grey");
					mc.gui.setScreen(new PauseScreen(true));
					wait = 30; step++;
				}
				case 19 -> { shot("08-dark-pause"); step = 40; }
				case 40 -> { mc.gui.setScreen(null); wait = 30; step++; }
				case 41 -> { shot("11-dark-hotbar"); step++; }
				case 42 -> {
					KeyMapping.click(InputConstants.getKey(mc.options.keyAdvancements.saveString()));
					wait = 5; step++;
				}
				case 43 -> {
					check(screen instanceof AdvancementsScreen, "the Advancements key (" + mc.options.keyAdvancements.saveString() + ") still opens Advancements; screen " + screen);
					wait = 20; step++;
				}
				case 44 -> { shot("12-dark-advancements"); step++; }
				case 45 -> { mc.gui.setScreen(new FriendsOverlayScreen(new PauseScreen(true))); wait = 30; step++; }
				case 46 -> { shot("13-dark-friends"); step++; }
				case 47 -> {
					((TabNavigationBar) readField(screen, "tabNavigationBar")).selectTab(1, false);
					wait = 20; step++;
				}
				case 48 -> { shot("14-dark-friend-requests"); step++; }
				case 49 -> { mc.gui.setScreen(new SocialInteractionsScreen()); wait = 30; step++; }
				case 50 -> { shot("15-dark-social"); step++; }
				case 51 -> {
					mc.gui.setScreen(null);
					UiConfig.get().hideTutorialToasts = false;
					mc.gui.toastManager().addToast(new TutorialToast(mc.font, TutorialToast.Icons.MOVEMENT_KEYS, Component.literal("Dark toast title"), Component.literal("lifted label text"), false));
					SystemToast.add(mc.gui.toastManager(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, Component.literal("System toast"), Component.literal("charcoal and crimson"));
					wait = 25; step++;
				}
				case 52 -> {
					check(DarkUi.remap(DarkUi.TOAST_PURPLE) == DarkUi.LIGHT_CRIMSON_TEXT, "toast purple title is lifted");
					shot("16-dark-toasts"); step++;
				}
				case 53 -> { UiConfig.get().hideTutorialToasts = true; step = 20; }
				case 20 -> {
					ScreenCleaner.SEEN.clear();
					mc.disconnectFromWorld(Component.literal("self-test"));
					step++;
				}
				case 21 -> {
					if (!(screen instanceof TitleScreen) || mc.level != null) return;
					writeExperimentalDatapack(mc);
					wait = 40; step++;
				}
				case 22 -> { shot("09-dark-title"); step++; }
				case 23 -> {
					ScreenCleaner.SEEN.clear();
					mc.createWorldOpenFlows().openWorld(WORLD, () -> mc.gui.setScreen(new TitleScreen()));
					step++;
				}
				case 24 -> {
					if (mc.player == null || mc.gui.screen() != null) {
						if (ticks % 40 == 0) UiCleanup.LOG.info("CLIENTTEST waiting to re-enter; screen {}", mc.gui.screen());
						if (mc.gui.screen() != null && mc.gui.screen().getClass().getSimpleName().contains("Backup")) {
							check(false, "a backup prompt appeared: " + mc.gui.screen().getClass().getName());
							shot("10-backup-prompt"); step = 26;
						}
						return;
					}
					var server = mc.getSingleplayerServer();
					Lifecycle lc = server == null ? null : server.getWorldData().worldGenSettingsLifecycle();
					check(lc != null && lc != Lifecycle.stable(), "re-opened world uses experimental settings (lifecycle " + lc + "), so vanilla would ask for a backup");
					check(UiCleanup.skippedBackupPrompts == 1, "backup prompt was skipped by the mod (" + UiCleanup.skippedBackupPrompts + ")");
					check(ScreenCleaner.SEEN.stream().noneMatch(n -> n.contains("BackupConfirmScreen") || n.endsWith("ConfirmScreen")),
						"re-opened the experimental world with no backup / experimental prompt; screens: " + ScreenCleaner.SEEN);
					wait = 40; step++;
				}
				case 25 -> { shot("10-world-reopened"); step++; }
				case 26 -> {
					setDark(mc, false);
					mc.disconnectFromWorld(Component.literal("self-test done"));
					step++;
				}
				case 27 -> {
					if (!(screen instanceof TitleScreen)) return;
					UiCleanup.LOG.info("CLIENTTEST SUMMARY\n  {}", String.join("\n  ", results));
					UiCleanup.LOG.info(ok ? "CLIENTTEST PASS" : "CLIENTTEST FAIL");
					step++;
					mc.stop();
				}
				default -> { }
			}
		} catch (Throwable t) {
			ok = false;
			UiCleanup.LOG.error("CLIENTTEST FAIL at step {}", step, t);
			UiCleanup.LOG.info("CLIENTTEST FAIL");
			step = 99;
			mc.stop();
		}
	}

	/** A one-file data pack with a custom dimension type: that is what makes a world "experimental settings" for vanilla. */
	private static void writeExperimentalDatapack(Minecraft mc) throws Exception {
		Path dp = mc.gameDirectory.toPath().resolve("saves/" + WORLD + "/datapacks/uic_test");
		Files.createDirectories(dp.resolve("data/uic_test/dimension_type"));
		Files.writeString(dp.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"UI Cleanup self-test\",\"min_format\":100,\"max_format\":130}}");
		String overworld;
		try (var in = Minecraft.class.getResourceAsStream("/data/minecraft/dimension_type/overworld.json")) {
			overworld = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
		}
		Files.writeString(dp.resolve("data/uic_test/dimension_type/test.json"), overworld);
		UiCleanup.LOG.info("CLIENTTEST wrote test data pack to {}", dp);
	}

	private static Object readField(Object o, String name) throws ReflectiveOperationException {
		var f = o.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(o);
	}
}
