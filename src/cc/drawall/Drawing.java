/*
 * This file is part of DraWall.
 * DraWall is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * DraWall is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with DraWall. If not, see <http://www.gnu.org/licenses/>.
 * © 2012–2014 Nathanaël Jourdane
 * © 2014 Victor Adam
 */

package cc.drawall;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** An in-memory representation of a vector image.
  * A Drawing is an ordered list of colored areas. Those areas are rendered
  * so that the latter ones can hide the first ones by drawing over them. */
public class Drawing implements Iterable<Drawing.Splash> {
	private static final Logger log = Logger.getLogger(Drawing.class.getName());

	/* Buffer used for temporary storage of coordinates. */
	private static final double[] coords = new double[6];

	/* The bounding rectangle of this Drawing (initially empty). */
	private Rectangle bounds = new Rectangle(0, 0, -1, -1);

	/* The splashes composing this Drawing. */
	private List<Splash> splashes = new ArrayList<>();

	/* Adds the specified shape, filled with the specified color, to this Drawing. */
	void paint(final Color color, final Shape shape) {
		bounds = bounds.union(shape.getBounds());
		splashes.add(new Splash(color, shape));
	}

	/* Changes the list of areas so that they can be rendered correctly in any order.
	 * This implies removing from the lower splashes parts that would be hidden by higher splashes. */
	void flatten() {
		final Area mask = new Area();
		final Map<Color, Area> colorMap = new HashMap<>();
		for (final Splash splash: splashes) {
			Area a = splash.shape instanceof Area ? (Area) splash.shape : new Area(splash.shape);
			a.subtract(mask); // TODO: eliminate empty segments generated by this
			if (a.isEmpty()) {
				return;
			}
			mask.add(a);
			if (colorMap.containsKey(splash.color)) {
				colorMap.get(splash.color).add(a);
			} else {
				colorMap.put(splash.color, a);
			}
		}
		splashes.clear();
		colorMap.forEach(this::paint);
	}

	/** Splits each Area into disjoint subpaths. */
	private void split() {
		final double[] start = {0, 0};
		final List<Splash> splitted = new ArrayList<>();
		forEach(splash -> {
			Path2D path = new Path2D.Double();
			for (final PathIterator itr = splash.getPathIterator(null); !itr.isDone(); itr.next()) {
				final int type = itr.currentSegment(coords);
				switch (type) {
				case PathIterator.SEG_MOVETO:
					path = new Path2D.Double();
					splitted.add(new Splash(splash.color, path));
					path.moveTo(coords[0], coords[1]);
					start[0] = coords[0];
					start[1] = coords[1];
					break;
				case PathIterator.SEG_LINETO:
					path.lineTo(coords[0], coords[1]);
					break;
				case PathIterator.SEG_QUADTO:
					path.quadTo(coords[0], coords[1], coords[2], coords[3]);
					break;
				case PathIterator.SEG_CUBICTO:
					path.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
					break;
				case PathIterator.SEG_CLOSE:
					path.lineTo(start[0], start[1]);
					break;
				default:
					assert false;
				}
			}
		});
		splashes = splitted;
	}

	/** Changes the order of shapes so as to reduce the total distance moved. */
	void optimize() {
		split();
		for (int i = 1; i < splashes.size(); i++) {
			double min = Double.POSITIVE_INFINITY;
			int best = -1;
			for (int j = i; j < splashes.size(); j++) {
				final double dist = getDistance(splashes.get(i - 1).shape, splashes.get(j).shape);
				best = dist < min ? j : best;
				min = dist < min ? dist : min;
			}
			if (best != i) {
				final Splash save = splashes.get(best);
				splashes.set(best, splashes.get(i));
				splashes.set(i, save);
			}
		}
	}

	/** Returns the bounding box of this Drawing. */
	public Rectangle getBounds() {
		return bounds;
	}

	@Override
	public Iterator<Splash> iterator() {
		return splashes.iterator();
	}

	public static class Splash {
		private static final Integer flatness = Integer.getInteger("flatness");
		public final Color color;
		public final Shape shape;
		public Splash(final Color color, final Shape shape) {
			this.color = color;
			this.shape = shape;
		}

		public PathIterator getPathIterator(final AffineTransform transform) {
			return flatness == null ? shape.getPathIterator(transform)
			                        : shape.getPathIterator(transform, flatness);
		}
	}

	/** Returns the distance between the first points of two Shapes. */
	private static double getDistance(Shape a, Shape b) {
		a.getPathIterator(null).currentSegment(coords);
		final double ax = coords[0], ay = coords[1];
		b.getPathIterator(null).currentSegment(coords);
		final double bx = coords[0], by = coords[1];
		return (bx - ax) * (bx - ax) + (by - ay) * (by - ay);
	}

	/** Returns the surface of the specified area. */
	public static double computeSurface(final Area area) {
		double surface = 0.0;
		double x = 0.0, y = 0.0, startX = 0.0, startY = 0.0, prevX, prevY;
		for (PathIterator i = area.getPathIterator(null, 0); !i.isDone(); i.next()) {
			prevX = x;
			prevY = y;
			final int segType = i.currentSegment(coords);
			if (segType == PathIterator.SEG_MOVETO) {
				x = startX = coords[0];
				y = startY = coords[1];
			} else {
				final boolean close = segType == PathIterator.SEG_CLOSE;
				x = close ? startX : coords[0];
				y = close ? startY : coords[1];
				surface += prevX * y - x * prevY;
			}
		}
		return Math.abs(surface) / 2;
	}

	/** Returns true iff the specified Drawing is identical to this one. */
	public boolean looksLike(final Drawing that) {
		log.warning("Entering looksLike");
		if (this.splashes.size() != that.splashes.size()) {
			log.warning("Sizes differ");
			return false;
		}
		for (int i = 0; i < splashes.size(); i++) {
			final Area xor = new Area(this.splashes.get(i).shape);
			final double totalSize = computeSurface(xor);
			xor.exclusiveOr((Area) that.splashes.get(i).shape);
			final double ratio = computeSurface(xor) / totalSize;
			log.warning("ratio: " + ratio);
			if (ratio > 1E-4) {
				return false;
			}
		}
		return true;
	}
}
