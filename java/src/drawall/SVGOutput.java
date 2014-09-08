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
import java.io.PrintWriter;
import java.util.Map;


public class SVGOutput implements Output {

	private static final String[] format = {"M %,%", "L %,%", "Q %,% %,%", "C %,% %,% %,%", "Z"};

	@Override
	public void output(Map<Color, Area> colorMap, AffineTransform transform, PrintWriter out) {
		out.println("<?xml version='1.0' encoding='UTF-8' standalone='no'?>");
		out.print("<!DOCTYPE svg PUBLIC '-//W3C//DTD SVG 1.0//EN' ");
		out.println("'http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd'>");
		out.println("<svg xmlns='http://www.w3.org/2000/svg' transform='translate(0 300) scale(1 -1)'>");
		colorMap.forEach((color, area) -> {
			out.format("<path style='fill:#%06x;stroke:none' d='", color.getRGB() & 0xFFFFFF);
			Utils.eachSegment(area, null, (coords, type) -> Utils.format(out, format[type], coords));
			out.print("'/>");
		});
		out.println("</svg>");
	}
}
