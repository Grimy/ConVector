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

import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.io.PrintStream;


public class GCodeOutput extends WriterGraphics2D {

	private static final String[] format = {"G00 X% Y%", "G01 X% Y%", "G5.1 I% J% X% Y%", "G05 I% J% P% Q% X% Y%"};

	@Override
	protected void output(AffineTransform transform, PrintStream out) {
		final double[] savedCoords = new double[2];

		colorMap.forEach((color, area) -> {
			PathIterator itr = flatness < 0 ? area.getPathIterator(transform)
			                                : area.getPathIterator(transform, flatness);
			for (; !itr.isDone(); itr.next()) {
				int type = itr.currentSegment(coords);
				if (type == 0) {
					savedCoords[0] = coords[0];
					savedCoords[1] = coords[1];
				} else if (type == 4) {
					coords[0] = savedCoords[0];
					coords[1] = savedCoords[1];
					type = 1;
				}
				WriterGraphics2D.format(out, format[type], coords);
			}
		});
		out.println("M30");
	}
}
