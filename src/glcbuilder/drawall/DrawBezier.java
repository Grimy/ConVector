/*
 * This file is part of DraWall.
 * DraWall is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * DraWall is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with DraWall. If not, see <http://www.gnu.org/licenses/>.
 * Copyright (c) 2012-2014 Nathanaël Jourdane.
 */

package drawall;


public class DrawBezier extends Instruction {
	/** Coordinates of the control points. */
	private double[] points;

	public DrawBezier(double... points) {
		assert points.length % 2 == 0; // TODO: use a Point[] instead?
		this.points = points;
	}

	@Override
	public String toGCode() {
		throw new UnsupportedOperationException(); // TODO
	}

	@Override
	public String toSVG() {
		return String.format("C %.3f,%.3f %.3f,%.3f %.3f,%.3f",
				points[0], points[1], points[2], points[3], points[4], points[5]);
	}
}
