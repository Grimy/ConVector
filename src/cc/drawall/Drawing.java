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
 * © 2014-2015 Victor Adam
 */

package cc.drawall;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/** An in-memory representation of a vector image.
  * A Drawing is an ordered list of colored areas. Those areas are rendered
  * so that the latter ones can hide the first ones by drawing over them. */
public class Drawing implements Output {
	private static final Logger log = Logger.getLogger(Drawing.class.getName());

	/* Buffer used for temporary storage of coordinates. */
	private final double[] coords = new double[6];

	/* The splashes composing this Drawing. */
	private List<Splash> splashes = new ArrayList<>();

	private Color color = Color.BLACK;

	private final Output delegate;
	private Path2D path;
	private final boolean merge;
	private final boolean optimize;

	public Drawing(Output delegate, boolean merge, boolean optimize) {
		this.delegate = delegate;
		this.merge = merge;
		this.optimize = optimize;
	}

	/* Changes the list of areas so that they can be rendered correctly in any order.
	 * This implies removing from the lower splashes parts that would be hidden by higher splashes. */
	private void mergeLayers() {
		log.info("Merging layers : " + splashes.size());
		final List<Splash> newSplashes = new ArrayList<>();
		for (final Splash top: splashes) {
			for (final Iterator<Splash> itr = newSplashes.iterator(); itr.hasNext();) {
				final Splash bottom = itr.next();
				if (!bottom.shape.intersects(top.shape.getBounds2D())) {
					continue;
				}
				final Area aTop = top.shape instanceof Area ? (Area) top.shape : new Area(top.shape);
				final Area aBottom = bottom.shape instanceof Area ? (Area) bottom.shape : new Area(bottom.shape);
				bottom.shape = aBottom;
				if (top.color.equals(bottom.color)) {
					aTop.add(aBottom);
					itr.remove();
				} else {
					aBottom.subtract(aTop);
					if (aBottom.isEmpty()) {
						itr.remove();
					}
				}
			}
			newSplashes.add(top);
		}
		splashes.clear();
		newSplashes.forEach(splashes::add);
		log.info("Done merging layers : " + splashes.size());
	}

	/* Changes the order of shapes so as to reduce the total distance moved. */
	void optimize() {
		final int numSplashes = splashes.size();
		for (int i = 1; i < numSplashes; i++) {
			double min = Double.POSITIVE_INFINITY;
			int best = -1;
			for (int j = i; j < numSplashes; j++) {
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

	@Override
	public void setSize(double width, double height) {
		delegate.setSize(width, height);
	}

	@Override
	public void writeColor(double red, double green, double blue) {
		color = new Color((float) red, (float) green, (float) blue);
	}

	@Override
	public void writeSegment(int type, double... coords) {
		switch (type) {
		case 0:
			if (path != null) {
				splashes.add(new Splash(color, path));
			}
			path = new Path2D.Float();
			path.moveTo(coords[0], coords[1]);
			break;
		case 1:
			path.lineTo(coords[0], coords[1]);
			break;
		case 2:
			path.quadTo(coords[0], coords[1], coords[2], coords[3]);
			break;
		case 3:
			path.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
			break;
		case 4:
			path.closePath();
			break;
		default:
			assert false;
		}
	}

	@Override
	public void writeFooter() {
		if (merge) {
			mergeLayers();
		}
		if (optimize) {
			optimize();
		}
		for (final Splash splash: splashes) {
			final Color color = splash.color;
			delegate.writeColor(color.getRed(), color.getGreen(), color.getBlue());
			for (final PathIterator itr = splash.iterator(null, -1);
					!itr.isDone(); itr.next()) {
				delegate.writeSegment(itr.currentSegment(coords), coords);
			}
		}
		delegate.writeFooter();
	}

	private static class Splash {
		public final Color color;
		public Shape shape;
		Splash(final Color color, final Shape shape) {
			this.color = color;
			this.shape = shape;
		}

		PathIterator iterator(final AffineTransform transform, final int flatness) {
			return flatness < 0 ? shape.getPathIterator(transform)
				: shape.getPathIterator(transform, flatness);
		}
	}

	/* Returns the distance between the first points of two Shapes. */
	private double getDistance(final Shape a, final Shape b) {
		a.getPathIterator(null).currentSegment(coords);
		final double ax = coords[0], ay = coords[1];
		b.getPathIterator(null).currentSegment(coords);
		final double bx = coords[0], by = coords[1];
		return (bx - ax) * (bx - ax) + (by - ay) * (by - ay);
	}

	/* Returns the surface of the specified area. */
	double computeSurface(final Area area) {
		double surface = 0.0;
		double x = 0.0, y = 0.0, startX = 0.0, startY = 0.0, prevX, prevY;
		for (final PathIterator i = area.getPathIterator(null, 0); !i.isDone(); i.next()) {
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
}
