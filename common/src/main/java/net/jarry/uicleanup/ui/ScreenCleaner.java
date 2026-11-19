package net.jarry.uicleanup.ui;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.jarry.uicleanup.UiCleanup;
import net.jarry.uicleanup.config.UiConfig;
import net.jarry.uicleanup.mixin.AbstractRecipeBookScreenAccessor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.jarry.uicleanup.platform.LoaderBridge;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Runs after any screen has built its widgets (Fabric ScreenEvents.AFTER_INIT / NeoForge ScreenEvent.Init.Post).
 * Removes the Realms, feedback/bug-report and player-reporting buttons wherever they appear, optionally the recipe
 * book button, then closes the gaps with {@link LayoutMath}. It only changes what this screen shows.
 */
public final class ScreenCleaner {
	/** Self-test hook: every screen class that got initialised, in order. */
	public static final List<String> SEEN = new java.util.concurrent.CopyOnWriteArrayList<>();

	static final int PAIR_GAP = 8;
	public static final int CORNER_MARGIN = 4;
	static final int ICON_GAP = 4;

	private ScreenCleaner() { }

	public static String key(Component c) {
		return c != null && c.getContents() instanceof TranslatableContents t ? t.getKey() : null;
	}

	public static void clean(Screen screen, List<? extends GuiEventListener> listeners, Consumer<GuiEventListener> remover) {
		SEEN.add(screen.getClass().getName());
		UiConfig cfg = UiConfig.get();
		Set<String> keys = RemovalRules.keys(cfg.removeRealms, cfg.removeFeedbackButtons, UiCleanup.chatReportRemovalActive());
		List<AbstractWidget> widgets = new ArrayList<>();
		for (GuiEventListener l : listeners) if (l instanceof AbstractWidget w) widgets.add(w);

		List<AbstractWidget> remove = new ArrayList<>();
		for (AbstractWidget w : widgets) {
			if (w instanceof AbstractButton && keys.contains(key(w.getMessage()))) remove.add(w);
			// Accessibility and Language icons on the title screen only; both stay reachable from Options.
			else if (screen instanceof TitleScreen && cfg.removeTitleAccessibilityLanguage && w instanceof AbstractButton
				&& key(w.getMessage()) != null && RemovalRules.TITLE_ACCESS_LANGUAGE.contains(key(w.getMessage()))) remove.add(w);
		}
		if (cfg.hideRecipeBookButton && screen instanceof AbstractRecipeBookScreen<?> rb
			&& !((AbstractRecipeBookScreenAccessor) rb).jarrys_ui_cleanup$recipeBook().isVisible()) {
			// The recipe book toggle is the only 20x18 image button on these screens. If the book is open we keep the
			// button so it can still be closed; we never flip the book's saved open/closed state ourselves.
			for (AbstractWidget w : widgets) {
				if (w.getClass() == ImageButton.class && w.getWidth() == 20 && w.getHeight() == 18) remove.add(w);
			}
		}
		// Pause menu: drop Advancements (the L key still opens it) and move the Friends icon into that row.
		AbstractWidget adv = null, stats = null, mover = null;
		List<AbstractWidget> movers = new ArrayList<>();
		if (screen instanceof PauseScreen && cfg.removeAdvancementsButton) {
			for (AbstractWidget w : widgets) {
				String k = key(w.getMessage());
				if (RemovalRules.ADVANCEMENTS.equals(k) && w instanceof AbstractButton) adv = w;
				else if (RemovalRules.STATISTICS.equals(k)) stats = w;
				else if (RemovalRules.FRIENDS.equals(k) && w.visible) mover = w;
			}
			if (adv != null) remove.add(adv);
			if (adv == null || stats == null || stats.getY() != adv.getY()) mover = null;
			if (mover != null) {
				// Everything left in the Friends icon row (Mods on NeoForge / Mod Menu, Player Reporting with No Chat
				// Reports) moves along, so that row empties and closes up instead of leaving one lonely icon.
				for (AbstractWidget w : widgets) {
					if (w.visible && w.getY() == mover.getY() && w.getWidth() == 20 && w.getHeight() == 20 && !remove.contains(w)) movers.add(w);
				}
				movers.sort(java.util.Comparator.comparingInt(AbstractWidget::getX));
			}
		}
		// Title screen: the rows made only of 20x20 icon buttons go to the bottom-left corner.
		List<AbstractWidget> cornerIcons = new ArrayList<>();
		if (screen instanceof TitleScreen && cfg.moveTitleIcons) cornerIcons = LayoutMath.iconOnlyRows(widgets, w -> w.visible && !remove.contains(w), AbstractWidget::getX, AbstractWidget::getY, AbstractWidget::getWidth, AbstractWidget::getHeight);
		if (remove.isEmpty() && cornerIcons.isEmpty()) return;
		int rowLeft = 0, rowRight = 0;
		if (mover != null) {
			rowLeft = Math.min(adv.getX(), stats.getX());
			rowRight = Math.max(adv.getX() + adv.getWidth(), stats.getX() + stats.getWidth());
		}

		Map<AbstractWidget, LayoutMath.Box> boxes = new IdentityHashMap<>();
		List<LayoutMath.Box> kept = new ArrayList<>(), removed = new ArrayList<>();
		for (AbstractWidget w : widgets) {
			if (!w.visible) continue;
			LayoutMath.Box b = new LayoutMath.Box(w.getX(), w.getY(), w.getWidth(), w.getHeight(), w instanceof Button.Plain);
			if (remove.contains(w) || movers.contains(w) || cornerIcons.contains(w)) removed.add(b); else { kept.add(b); boxes.put(w, b); }
		}
		for (AbstractWidget w : remove) remover.accept(w);
		if (screen instanceof AbstractRecipeBookScreen<?>) return; // nothing to re-flow there
		LayoutMath.close(kept, removed, screen.height);
		for (Map.Entry<AbstractWidget, LayoutMath.Box> e : boxes.entrySet()) {
			AbstractWidget w = e.getKey();
			LayoutMath.Box b = e.getValue();
			if (w.getX() != b.x || w.getY() != b.y) w.setPosition(b.x, b.y);
			if (w.getWidth() != b.w) w.setWidth(b.w);
		}
		if (!cornerIcons.isEmpty()) {
			cornerIcons.sort(java.util.Comparator.comparingInt(AbstractWidget::getY).thenComparingInt(AbstractWidget::getX));
			int y = screen.height - 10 * LoaderBridge.titleBrandingLines() - CORNER_MARGIN - 20;
			int x = CORNER_MARGIN;
			for (AbstractWidget w : cornerIcons) { w.setPosition(x, y); x += w.getWidth() + CORNER_MARGIN; }
		}
		if (mover != null) {
			// Same left and right edges as the other rows; the usual 8 px pause-menu gap between Statistics and the icon.
			int y = stats.getY();
			int iconsWidth = movers.size() * 20 + (movers.size() - 1) * ICON_GAP;
			stats.setPosition(rowLeft, y);
			stats.setWidth(rowRight - rowLeft - PAIR_GAP - iconsWidth);
			int x = rowRight - iconsWidth;
			for (AbstractWidget w : movers) { w.setPosition(x, y); x += 20 + ICON_GAP; }
		}
	}
}
