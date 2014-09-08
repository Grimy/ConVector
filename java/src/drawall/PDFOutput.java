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

public class PDFOutput implements Output {

	private static final String[] format = {"% % m", "% % l", null, "% % % % % % c", "h"};

	@Override
	public void output(Map<Color, Area> colorMap, AffineTransform transform, PrintWriter out) {
		out.println("%PDF-1");
		out.println("1 0 obj<</Pages 1 0 R/Kids[2 0 R]/Count 1>>endobj");
		out.println("2 0 obj<</Contents 3 0 R/MediaBox[0 0 300 300]>>endobj");
		out.println("3 0 obj<</Length 0>>stream");
		colorMap.forEach((color, area) -> {
			out.format("%f %f %f rg\n", color.getRed() / 255.0, color.getGreen() / 255.0, color.getBlue() / 255.0);
			Utils.eachSegment(area, null, (coords, type) -> Utils.format(out, format[type], coords));
			out.println("h f");
		});
		out.println("endstream endobj trailer<</Size 0/Root 1 0 R>>");
	}
}
