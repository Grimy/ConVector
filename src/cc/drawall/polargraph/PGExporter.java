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

import java.nio.ByteBuffer;

import cc.drawall.Exporter;

/** Outputs a vector to PostScript code. */
public class PGExporter extends Exporter {

	private static final int WIDTH = Integer.getInteger("polargraph.width", 7500);
	private static final double RATIO = WIDTH / 65535.0;

	// super(FLATTEN | MERGE | SHORTEN);

	@Override
	protected ByteBuffer header(final double width, final double height, final double ratio) {
		return EMPTY;
	}

	@Override
	protected ByteBuffer segment(final int type, final double[] coords) {
		final double x = coords[0] * RATIO, y = coords[1] * RATIO;
		return format(
			type == 0 ? "C14,END\nC17,%d,%d,END\nC13,END\n" :  "C17,%d,%d,END\n",
			(int) Math.sqrt(x * x + y * y),
			(int) Math.sqrt((WIDTH - x) * (WIDTH - x) + y * y));
	}
}
