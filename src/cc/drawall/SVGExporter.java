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
import java.io.IOException;

public class SVGExporter extends Exporter {

	public SVGExporter() {
		super("M%,%", "L%,%", "Q%,% %,%", "C%,% %,% %,%", "Z");
	}

	@Override
	protected AffineTransform writeHeader(final Drawing drawing) throws IOException {
		final Rectangle bounds = drawing.getBounds();
		out.writeBytes("<?xml version='1.0' encoding='UTF-8' standalone='no'?>\n");
		out.writeBytes("<!DOCTYPE svg PUBLIC '-//W3C//DTD SVG 1.0//EN' ");
		out.writeBytes("'http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd'>\n");
		out.writeBytes("<svg xmlns='http://www.w3.org/2000/svg'>\n<metadata id='");
		return new AffineTransform(1, 0, 0, 1, -bounds.x, -bounds.y);
	}

	@Override
	protected void writeColor(final Color color) throws IOException {
		writeFormat("'/><path style='fill:#%06x;stroke:none' d='", color.getRGB() & 0xFFFFFF);
	}

	@Override
	protected void writeFooter() throws IOException {
		out.writeBytes("'/></svg>\n");
	}
}
