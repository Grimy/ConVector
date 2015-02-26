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
 * © 2014-2015 Victor Adam
 */

package cc.drawall.ps;

import cc.drawall.Exporter;

/** Outputs a vector to PostScript code. */
public class PSExporter extends Exporter {

	/** Constructor. */
	public PSExporter() {
		super(REVERSE, "% % m", "% % l", "% % % % q", "% % % % % % c", "h");
	}

	@Override
	protected void writeHeader(final double width, final double height, final double ratio) {
		write("%%!PS\n"
			+ "%% Lovingly %s\n"
			+ "%%%%BoundingBox: 0 0 %d %d\n"
			+ "/d{load def}bind def/m/moveto d/l/lineto d/c/curveto d"
			+ "/h/closepath d/f/fill d/rg/setrgbcolor d\n"
			+ "/Z{3 1 roll add 3 div}def\n/q{4 2 roll 2 mul exch 2 mul 2 copy "
			+ "currentpoint Z Z 6 2 roll 3 index 3 index Z Z 4 2 roll c}def\n"
			+ "%f %f scale\n", COMMENT, (int) width, (int) height, ratio, ratio);
	}

	@Override
	protected void writeColor(final double red, final double green, final double blue) {
		write("f %.3f %.3f %.3f rg\n", red, green, blue);
	}

	@Override
	protected void writeFooter() {
		write("f\n");
	}
}
