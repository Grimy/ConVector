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

package cc.drawall.dov;

import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

import cc.drawall.Exporter;

/** Outputs a vector to DOV format. */
public class DOVExporter extends Exporter {

	public DOVExporter() {
		super(FLATTEN | MERGE | SHORTEN);
	}

	@Override
	protected void writeHeader(final Rectangle2D bounds) throws IOException {
		write("#\u0039\u00FF\u00AF");
		writeChar((int) bounds.getWidth());
		writeChar((int) bounds.getHeight());
	}

	@Override
	protected void writeSegment(final int type, final double[] coords) throws IOException {
		switch (type) {
		case PathIterator.SEG_MOVETO:
			writeChar(0x0001);
			writeChar((int) coords[0]);
			writeChar((int) coords[1]);
			break;
		case PathIterator.SEG_LINETO:
			writeChar((int) coords[0]);
			writeChar((int) coords[1]);
			break;
		case PathIterator.SEG_CLOSE:
			break;
		default:
			assert false : "Unexpected segment type; try setting -Dflatness";
		}
	}
}
