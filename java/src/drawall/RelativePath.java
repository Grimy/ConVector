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
import java.util.Arrays;

public class RelativePath extends Path2D.Double {

	// XXX: shared mutable state
	// Allows for PS path where the matrix changes in the middle of a path.
	private final AffineTransform ctm;

	public RelativePath(AffineTransform ctm) {
		this.ctm = ctm;
	}

	public RelativePath(RelativePath that) {
		this.ctm = that.ctm;
		this.append(that, false);
	}

	public void moveTo(boolean relative, double... points) {
		transform(relative, points, 1);
		super.moveTo(points[0], points[1]);
	}

	public void lineTo(boolean relative, double... points) {
		transform(relative, points, 1);
		super.lineTo(points[0], points[1]);
	}

	public void quadTo(boolean relative, double... points) {
		transform(relative, points, 2);
		super.quadTo(points[0], points[1], points[2], points[3]);
	}

	public void curveTo(boolean relative, double... points) {
		transform(relative, points, 3);
		super.curveTo(points[0], points[1], points[2], points[3], points[4], points[5]);
	}

	// TODO : test this with a translate ctm and relative on
	private void transform(boolean relative, double[] points, int nbPoints) {
		assert points.length == 2 * nbPoints;
		ctm.transform(points, 0, points, 0, nbPoints);
		System.out.println(Arrays.toString(points));
		Point2D p = super.getCurrentPoint();
		if (relative) {
			assert p != null;
			for (int i = 0; i < 2 * nbPoints;) {
				points[i++] += p.getX();
				points[i++] += p.getY();
			}
			System.out.println("Relativized: " + Arrays.toString(points));
		}
		if (p != null) {
			points[0] = java.lang.Double.isNaN(points[0]) ? p.getX() : points[0];
			points[1] = java.lang.Double.isNaN(points[1]) ? p.getY() : points[1];
		}
	}
}
