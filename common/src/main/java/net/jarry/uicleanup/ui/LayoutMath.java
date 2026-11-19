package net.jarry.uicleanup.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure layout maths used after buttons are taken out of a screen, so the remaining buttons close the gap.
 *
 * <ul>
 * <li>If a removed button shared its row with other buttons, the survivors of that row are re-centred on the
 *     row's original centre, keeping the original spacing. A lone full-width-row survivor that is a text button is
 *     stretched to the row's original span (e.g. "Mods" next to a removed half-width Realms button).</li>
 * <li>If a removed button had its row to itself, every button below it in the same column is moved up by the
 *     distance to the next row, so there is no hole. Buttons pinned to the bottom edge (like the copyright text) stay put.</li>
 * </ul>
 */
public final class LayoutMath {
	/**
	 * Widgets that sit in rows made only of 20x20 icon buttons (every widget on that row is 20x20). Rows that also hold
	 * a normal button (for example Singleplayer with a small icon beside it) are left alone.
	 */
	public static <W> List<W> iconOnlyRows(List<W> all, java.util.function.Predicate<W> use, java.util.function.ToIntFunction<W> x,
			java.util.function.ToIntFunction<W> y, java.util.function.ToIntFunction<W> w, java.util.function.ToIntFunction<W> h) {
		java.util.Map<Integer, List<W>> rows = new java.util.TreeMap<>();
		for (W o : all) if (use.test(o)) rows.computeIfAbsent(y.applyAsInt(o), k -> new java.util.ArrayList<>()).add(o);
		List<W> out = new java.util.ArrayList<>();
		for (List<W> row : rows.values()) {
			if (row.stream().allMatch(o -> w.applyAsInt(o) == 20 && h.applyAsInt(o) == 20)) out.addAll(row);
		}
		return out;
	}

	public static final int BOTTOM_PIN = 16;

	private LayoutMath() { }

	public static final class Box {
		public int x, y, w, h;
		public final boolean stretchable;

		public Box(int x, int y, int w, int h, boolean stretchable) {
			this.x = x; this.y = y; this.w = w; this.h = h; this.stretchable = stretchable;
		}

		public Box(int x, int y, int w, int h) { this(x, y, w, h, false); }

		int right() { return x + w; }
		int bottom() { return y + h; }

		boolean sameRow(Box o) { return o.y == y && o.h == h; }

		@Override public String toString() { return "[" + x + "," + y + " " + w + "x" + h + "]"; }
	}

	/** Moves the boxes in {@code kept} in place; {@code removed} are the boxes that were taken out. */
	public static void close(List<Box> kept, List<Box> removed, int screenHeight) {
		// Handle the lowest removed rows first so upward shifts never move a box past another removed slot.
		List<Box> order = new ArrayList<>(removed);
		order.sort((a, b) -> Integer.compare(b.y, a.y));
		List<Box> handledRows = new ArrayList<>();
		for (Box r : order) {
			if (handledRows.stream().anyMatch(r::sameRow)) continue;
			List<Box> rowRemoved = removed.stream().filter(r::sameRow).toList();
			List<Box> rowKept = rowGroup(rowRemoved, kept);
			handledRows.add(r);
			if (!rowKept.isEmpty()) {
				recentre(rowKept, rowRemoved);
			} else {
				int left = rowRemoved.stream().mapToInt(x -> x.x).min().orElse(r.x);
				int right = rowRemoved.stream().mapToInt(Box::right).max().orElse(r.right());
				collapse(kept, new Box(left, r.y, right - left, r.h), screenHeight);
			}
		}
	}

	/** Kept boxes on the same row that are chained to a removed box by gaps of at most 12 px. */
	private static List<Box> rowGroup(List<Box> rowRemoved, List<Box> kept) {
		List<Box> group = new ArrayList<>(rowRemoved);
		List<Box> out = new ArrayList<>();
		boolean grew = true;
		while (grew) {
			grew = false;
			for (Box k : kept) {
				if (out.contains(k) || !k.sameRow(rowRemoved.get(0))) continue;
				for (Box g : group) {
					if (gap(k, g) <= 12) { out.add(k); group.add(k); grew = true; break; }
				}
			}
		}
		return out;
	}

	private static int gap(Box a, Box b) {
		if (a.right() <= b.x) return b.x - a.right();
		if (b.right() <= a.x) return a.x - b.right();
		return 0;
	}

	private static void recentre(List<Box> rowKept, List<Box> rowRemoved) {
		List<Box> all = new ArrayList<>(rowKept);
		all.addAll(rowRemoved);
		all.sort((a, b) -> Integer.compare(a.x, b.x));
		int left = all.get(0).x;
		int right = all.stream().mapToInt(Box::right).max().orElse(left);
		int spacing = 4;
		for (int i = 1; i < all.size(); i++) {
			int g = all.get(i).x - all.get(i - 1).right();
			if (g >= 0) { spacing = g; break; }
		}
		List<Box> keep = new ArrayList<>(rowKept);
		keep.sort((a, b) -> Integer.compare(a.x, b.x));
		if (keep.size() == 1 && keep.get(0).stretchable && right - left >= 150) {
			keep.get(0).x = left;
			keep.get(0).w = right - left;
			return;
		}
		int total = keep.stream().mapToInt(b -> b.w).sum() + spacing * (keep.size() - 1);
		int centre = (left + right) / 2;
		int x = centre - total / 2;
		for (Box b : keep) {
			b.x = x;
			x += b.w + spacing;
		}
	}

	private static void collapse(List<Box> kept, Box r, int screenHeight) {
		int colLeft = r.x - 40, colRight = r.right() + 40;
		int next = Integer.MAX_VALUE;
		for (Box b : kept) {
			if (b.y >= r.bottom() && overlapsX(b, colLeft, colRight) && !pinned(b, screenHeight)) next = Math.min(next, b.y);
		}
		if (next == Integer.MAX_VALUE) return;
		int delta = next - r.y;
		for (Box b : kept) {
			if (b.y >= r.bottom() && overlapsX(b, colLeft, colRight) && !pinned(b, screenHeight)) b.y -= delta;
		}
	}

	private static boolean overlapsX(Box b, int l, int r) {
		return b.right() > l && b.x < r;
	}

	private static boolean pinned(Box b, int screenHeight) {
		return b.bottom() > screenHeight - BOTTOM_PIN;
	}
}
