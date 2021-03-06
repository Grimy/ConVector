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

package cc.drawall.gcode;

import java.nio.ByteBuffer;

import cc.drawall.Exporter;

/** Outputs a vector to GCode. */
public class GCodeExporter extends Exporter {

	public GCodeExporter() {
		/* MERGE | SHORTEN | FLATTEN | REVERSE */
		super("G0 X% Y%", "G1 X% Y%", "G5.1 I% J% X% Y%", "G5 I% J% P% Q% X% Y%", "");
	}

	@Override
	protected ByteBuffer header(final double width, final double height, final double ratio) {
		return format("; %dx%d\n; Neatly %s\n", (int) width, (int) height, COMMENT);
	}
}
