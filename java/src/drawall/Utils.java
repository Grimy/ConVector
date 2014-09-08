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

package drawall;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Stack;
import java.util.function.ObjIntConsumer;

public abstract class Utils {

	private static double[] coords = new double[6]; // buffer

	private Utils() {
		// This class cannot be extended
	}

	/** Replaces each '%' character in the input with an element from `args`. */
	public static void format(PrintWriter out, String template, double... args) {
		// Irritatingly, String.format treats a double[] as a single Object.
		// Converting to a Double[] works, but is cumbersome and terrible for perf.
		// That’s why we have our own implementation of this.
		int i = 0;
		for (char c: template.toCharArray()) {
			if (c == '%') {
				out.print((int) args[i++]);
			} else {
				out.print(c);
			}
		}
		out.println();
	}

	/** Returns the distance between the first points of two Shapes. */
	public static double getDistance(Shape a, Shape b) {
		a.getPathIterator(null).currentSegment(coords);
		double ax = coords[0], ay = coords[1];
		b.getPathIterator(null).currentSegment(coords);
		double bx = coords[0], by = coords[1];
		return (bx - ax) * (bx - ax) + (by - ay) * (by - ay);
	}

	/** Calls a function for each segment in a given Shape). */
	public static void eachSegment(Shape shape, AffineTransform transform, ObjIntConsumer<double[]> callback) {
		for (PathIterator i = shape.getPathIterator(transform); !i.isDone(); i.next()) {
			int type = i.currentSegment(coords);
			callback.accept(coords, type);
		}
	}

	/** Returns an array of subpaths in an order that reduces total distance  moved. */
	public static Path2D[] optimize(Map<Color, Area> colorMap) {
		Stack<Path2D> paths = new Stack<>();
		double[] start = {0, 0};
		paths.add(new Path2D.Double());

		// Splits each area in disjoint subpaths.
		for (Area area: colorMap.values()) {
			Utils.eachSegment(area, null, (coords, type) -> {
				switch (type) {
					case PathIterator.SEG_MOVETO:
						paths.peek().moveTo(coords[0], coords[1]);
						start[0] = coords[0];
						start[1] = coords[1];
						break;
					case PathIterator.SEG_LINETO:
						paths.peek().lineTo(coords[0], coords[1]);
						break;
					case PathIterator.SEG_QUADTO:
						paths.peek().quadTo(coords[0], coords[1], coords[2], coords[3]);
						break;
					case PathIterator.SEG_CUBICTO:
						paths.peek().curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
						break;
					case PathIterator.SEG_CLOSE:
						paths.peek().lineTo(start[0], start[1]);
						paths.add(new Path2D.Double());
						paths.peek().moveTo(start[0], start[1]);
						break;
					default:
						assert false;
				}
			});
		}

		Path2D[] optimized = new Path2D[0];
		optimized = paths.toArray(optimized);
		for (int i = 1; i < optimized.length; i++) {
			double min = Double.POSITIVE_INFINITY;
			int best = -1;
			for (int j = i; j < optimized.length; j++) {
				double dist = Utils.getDistance(optimized[i - 1], optimized[j]);
				if (dist < min) {
					min = dist;
					best = j;
				}
			}
			if (best != i) {
				Path2D tmp = optimized[best];
				optimized[best] = optimized[i];
				optimized[i] = tmp;
			}
		}

		return optimized;
	}

	// public double computeSurface(Area a) {
		// PathIterator i = a.getPathIterator(null, 0);
		// double surface = 0.0;
		// double x = 0.0, y = 0.0, startX = 0.0, startY = 0.0, prevX, prevY;
		// for (; !i.isDone(); i.next()) {
			// prevX = x;
			// prevY = y;
			// int segType = i.currentSegment(coords);
			// if (segType == PathIterator.SEG_MOVETO) {
				// x = startX = coords[0];
				// y = startY = coords[1];
			// } else {
				// boolean close = segType == PathIterator.SEG_CLOSE;
				// x = close ? startX : coords[0];
				// y = close ? startY : coords[1];
				// surface += prevX * y - x * prevY;
			// }
		// }
		// return Math.abs(surface) / 2;
	// }

}
