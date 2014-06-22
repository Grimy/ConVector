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


public class DrawArc extends Instruction {
	/** Coordinates of the end of this arc. */
	private double x, y;

	/** Coordinates of the center of this arc. */
	private double i, j;

	/** true to move clockwise, false to move counterclockwise. */
	private boolean clockwise;

	public DrawArc(double x, double y, double i, double j, boolean clockwise) {
		this.x = x;
		this.y = y;
		this.i = i;
		this.j = j;
		this.clockwise = clockwise;
	}

	@Override
	public String toGCode() {
		return (clockwise ? "G02" : "G03") + String.format(" X%.3f Y%.3f I%.3f J%.3f", x, y, i, j);
	}

	@Override
	public String toSVG() {
		// TODO
		throw new UnsupportedOperationException("Converting arcs to SVG is not yet supported");
	}
}
