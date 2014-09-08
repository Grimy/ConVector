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
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.io.PrintWriter;
import java.util.Map;


public class GCodeOutput implements Output {

	private static final String[] format = {"G00 X% Y%", "G01 X% Y%", "G5.1 I% J% X% Y%", "G05 I% J% P% Q% X% Y%"};

	@Override
	public void output(Map<Color, Area> colorMap, AffineTransform transform, PrintWriter out) {
		out.println("G21");
		for (Path2D path: Utils.optimize(colorMap)) {
			Utils.eachSegment(path, transform, (coords, type) -> Utils.format(out, format[type], coords));
		}
		out.println("M30");
	}
}
