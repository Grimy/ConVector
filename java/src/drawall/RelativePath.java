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

import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

/** Adds method for relative movement to Sun's Path. */
public class RelativePath extends Path2D.Double {

	public AffineTransform ctm = new AffineTransform();

	public void rLineTo(double x, double y) {
		Point2D p = getCurrentPoint();
		assert p != null;
		System.out.println("lineto: " + (p.getX() + x) + ", " + (p.getY() + y));
		lineTo(p.getX() + x(x, y), p.getY() + y());
	}

	private Point2D transformed = new Point2D.Double();
	private double x(double x, double y) {
		ctm.transform(new Point2D.Double(x, y), transformed);
		return transformed.getX();
	}

	private double y() {
		return transformed.getY();
	}
}
