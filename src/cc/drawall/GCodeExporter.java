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


public class GCodeExporter extends Exporter {

	public GCodeExporter() {
		super("G00 X% Y%", "G01 X% Y%", "G5.1 I% J% X% Y%", "G5 I% J% P% Q% X% Y%");
	}

	@Override
	protected AffineTransform writeHeader(final Drawing drawing) throws IOException {
		// TODO : return flags requiring optimization and flattening
		drawing.optimize();
		final Rectangle bounds = drawing.getBounds();
		final double ratio = 25000.0 / Math.max(bounds.width, bounds.height);
		out.writeBytes("G21\n");
		return new AffineTransform(ratio, 0, 0, -ratio,
				-bounds.x * ratio, (bounds.height + bounds.y) * ratio);
	}

	@Override
	protected void writeColor(final Color color) {
		/* Colored output is not supported */
	}

	@Override
	protected void writeFooter() throws IOException {
		out.writeBytes("M30\n");
	}
}
