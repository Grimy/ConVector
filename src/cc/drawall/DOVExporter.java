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

package cc.drawall;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.io.IOException;

/** An Exporter for the DOV format. */
public class DOVExporter extends Exporter {

	@Override
	protected AffineTransform writeHeader(final Drawing drawing) throws IOException {
		final Rectangle bounds = drawing.getBounds();
		final double ratio = 65535.0 / Math.max(bounds.width, bounds.height);
		out.writeChars("\u39FF\u0000");
		out.writeChar((int) (bounds.width * ratio));
		out.writeChar((int) (bounds.height * ratio));
		out.writeChar(bounds.width);
		out.writeChar(bounds.height);
		return new AffineTransform(ratio, 0, 0, ratio, -bounds.x * ratio, -bounds.y * ratio);
	}

	@Override
	protected void writeColor(final Color color) throws IOException {
		out.writeChar(0x10C0);
		out.writeInt(color.getRGB());
	}

	@Override
	protected void writeSegment(final int type, final double[] coords) throws IOException {
		switch (type) {
		case PathIterator.SEG_MOVETO:
			out.writeChar(0x0001);
			out.writeChar((int) coords[0]);
			out.writeChar((int) coords[1]);
			break;
		case PathIterator.SEG_LINETO:
			out.writeChar((int) coords[0]);
			out.writeChar((int) coords[1]);
			break;
		case PathIterator.SEG_CLOSE:
			break;
		default:
			assert false : "Unexpected segment type; try setting -Dflatness";
		}
	}

	@Override
	protected void writeFooter() throws IOException {
		out.writeChar(0xFFFF);
	}
}
