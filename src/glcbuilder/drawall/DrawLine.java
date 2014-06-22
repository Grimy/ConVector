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


public class DrawLine extends Instruction {
	/** Coordinates of the end of this line. */
	private double x, y, z;

	/** When false, move without actually drawing. */
	private boolean write;

	public DrawLine(double x, double y, double z, boolean write) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.write = write;
	}

	public DrawLine(double[] points, boolean write) {
		this(points[0], points[1], 0, write);
	}

	@Override
	public String toGCode() {
		return (write ? "G01" : "G00") + String.format(" X%.3f Y%.3f Z%.3f", x, y, z);
	}

	@Override
	public String toSVG() {
		return (write ? "L " : "M ") + String.format("%.3f,%.3f", x, y);
	}
}
