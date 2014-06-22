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

import java.util.Vector;

public class PSGraphics {
	PSGraphics prev = null;

	double[] ctm = idMatrix();
	Vector<Instruction> path = new Vector<>();
	byte linecap = 0; // butt, round, projecting
	byte linejoin = 0; // Miter, round, Bevel
	double miterLimit = 10;
	double linewidth = 1;

	// clip path
	// raster output device

	PSGraphics dup() {
		PSGraphics clone = new PSGraphics();
		clone.ctm = ctm.clone();
		clone.path.addAll(path);
		clone.linecap = linecap;
		clone.linejoin = linejoin;
		clone.miterLimit = miterLimit;
		clone.linewidth = linewidth;
		clone.prev = this;
		return clone;
	}

	static double[] idMatrix() {
		return new double[]{1.0, 0.0, 0.0, 1.0, 0.0, 0.0};
	}
}
