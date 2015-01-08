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

package cc.drawall.ps;

import java.awt.Color;

import cc.drawall.Exporter;

/** Outputs a vector to PostScript code. */
public class PSExporter extends Exporter {

	/** Constructor. */
	public PSExporter() {
		super(REVERSE, "% % m", "% % l", null, "% % % % % % c", "h");
	}

	@Override
	protected void writeHeader(final double width, final double height, final double ratio) {
		write("%%!PS\n");
		write("%%%%BoundingBox: 0 0 %d %d\n", (int) width, (int) height);
		write("%% Lovingly generated by ConVector\n");
		write("/d{load def}bind def/m/moveto d/l/lineto d/c/curveto d");
		write("/h/closepath d/f/fill d/rg/setrgbcolor d\n");
		write("%f %f scale\n", ratio, ratio);
	}

	@Override
	protected void writeColor(final Color color) {
		final float[] rgb = color.getRGBColorComponents(null);
		write("f %.3f %.3f %.3f rg\n", rgb[0], rgb[1], rgb[2]);
	}

	@Override
	protected void writeFooter() {
		write("f\n");
	}
}
