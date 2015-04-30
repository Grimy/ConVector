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

package cc.drawall.polargraph;

import cc.drawall.Exporter;

/** Outputs a vector to PostScript code. */
public class PGExporter extends Exporter {

	private static final int WIDTH = Integer.getInteger("polargraph.width");
	private static final double RATIO = WIDTH / 65535.0;

	/** Constructor. */
	public PGExporter() {
		super(FLATTEN | MERGE | SHORTEN);
	}

	@Override
	protected void writeHeader(final double width, final double height, final double ratio) {
	}

	@Override
	protected void writeSegment(final int type, final double[] coords) {
		if (type == 0) {
			write("C14,END\n");
		}
		final double x = coords[0] * RATIO, y = coords[1] * RATIO;
		final int a = (int) Math.sqrt(x * x + y * y);
		final int b = (int) Math.sqrt((WIDTH - x) * (WIDTH - x) + y * y);
		write("C17,%d,%d,END\n", a, b);
		if (type == 0) {
			write("C13,END\n");
		}
	}

	@Override
	protected void writeColor(final double red, final double green, final double blue) {
	}

	@Override
	protected void writeFooter() {
	}
}
