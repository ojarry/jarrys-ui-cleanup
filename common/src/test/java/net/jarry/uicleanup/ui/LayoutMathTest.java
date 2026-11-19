package net.jarry.uicleanup.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import net.jarry.uicleanup.ui.LayoutMath.Box;
import org.junit.jupiter.api.Test;

class LayoutMathTest {
	@Test
	void titleScreenRealmsRowCollapses() {
		// 427x240 GUI, vanilla title layout: singleplayer 108, multiplayer 132, realms 156, icons 180, options/quit 204
		Box sp = new Box(113, 108, 200, 20), mp = new Box(113, 132, 200, 20), realms = new Box(113, 156, 200, 20);
		Box friends = new Box(179, 180, 20, 20), lang = new Box(203, 180, 20, 20), acc = new Box(227, 180, 20, 20);
		Box opt = new Box(113, 204, 98, 20), quit = new Box(215, 204, 98, 20), copyright = new Box(300, 230, 120, 10);
		List<Box> kept = new ArrayList<>(List.of(sp, mp, friends, lang, acc, opt, quit, copyright));
		LayoutMath.close(kept, List.of(realms), 240);
		assertEquals(108, sp.y);
		assertEquals(132, mp.y);
		assertEquals(156, friends.y);
		assertEquals(156, acc.y);
		assertEquals(180, opt.y);
		assertEquals(180, quit.y);
		assertEquals(230, copyright.y, "bottom-pinned copyright stays");
	}

	@Test
	void pauseIconRowRecentresWithoutGaps() {
		// row centre 213: bug, feedback, friends, report at 20px + 4px spacing
		Box bug = new Box(167, 100, 20, 20), fb = new Box(191, 100, 20, 20), fr = new Box(215, 100, 20, 20), rep = new Box(239, 100, 20, 20);
		List<Box> kept = new ArrayList<>(List.of(fr));
		LayoutMath.close(kept, List.of(bug, fb, rep), 240);
		assertEquals(203, fr.x, "single survivor centred on the old row centre");
		assertEquals(100, fr.y);
	}

	@Test
	void pauseIconRowWithModsButtonKeepsSpacing() {
		Box mods = new Box(155, 100, 20, 20), bug = new Box(179, 100, 20, 20), fb = new Box(203, 100, 20, 20), fr = new Box(227, 100, 20, 20), rep = new Box(251, 100, 20, 20);
		List<Box> kept = new ArrayList<>(List.of(mods, fr, rep));
		LayoutMath.close(kept, List.of(bug, fb), 240);
		assertEquals(24, fr.x - mods.x);
		assertEquals(24, rep.x - fr.x);
		int centre = (155 + 271) / 2;
		assertEquals(centre, (mods.x + rep.x + rep.w) / 2, 1);
	}

	@Test
	void halfWidthNeighbourStretchesToFullRow() {
		Box mods = new Box(113, 156, 98, 20, true), realms = new Box(215, 156, 98, 20);
		List<Box> kept = new ArrayList<>(List.of(mods));
		LayoutMath.close(kept, List.of(realms), 240);
		assertEquals(113, mods.x);
		assertEquals(200, mods.w);
	}

	@Test
	void unrelatedColumnsDoNotMove() {
		Box removed = new Box(10, 50, 60, 20), below = new Box(10, 80, 60, 20), farAway = new Box(300, 80, 60, 20);
		List<Box> kept = new ArrayList<>(List.of(below, farAway));
		LayoutMath.close(kept, List.of(removed), 240);
		assertEquals(50, below.y);
		assertEquals(80, farAway.y);
	}

	@Test
	void iconOnlyRowsSkipsMixedRows() {
		record W(String n, int x, int y, int w, int h) { }
		List<W> all = List.of(new W("single", 100, 100, 200, 20), new W("tw", 302, 100, 20, 20),
			new W("lang", 150, 150, 20, 20), new W("acc", 174, 150, 20, 20), new W("opts", 100, 174, 98, 20));
		List<W> icons = LayoutMath.iconOnlyRows(all, o -> true, W::x, W::y, W::w, W::h);
		org.junit.jupiter.api.Assertions.assertEquals(List.of("lang", "acc"), icons.stream().map(W::n).toList());
	}

	@Test
	void emptiedIconRowCollapsesBothColumns() {
		// Pause menu on NeoForge: the whole icon row goes, the leftmost icon is far from the right-hand column.
		Box opt = new Box(111, 140, 98, 20, true), wo = new Box(217, 140, 98, 20, true), save = new Box(111, 164, 204, 20, true);
		List<Box> kept = new ArrayList<>(List.of(new Box(111, 68, 204, 20, true), opt, wo, save));
		List<Box> removed = new ArrayList<>(List.of(new Box(155, 116, 20, 20), new Box(179, 116, 20, 20), new Box(203, 116, 20, 20),
			new Box(227, 116, 20, 20), new Box(251, 116, 20, 20)));
		LayoutMath.close(kept, removed, 240);
		assertEquals(116, opt.y);
		assertEquals(116, wo.y);
		assertEquals(140, save.y);
	}
}
