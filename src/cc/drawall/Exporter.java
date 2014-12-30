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
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract class Exporter {

	private static final double[] coords = new double[6]; // buffer
	protected static final int REVERSE = 1 << 0;
	protected static final int FLATTEN = 1 << 1;
	protected static final int SHORTEN = 1 << 2;
	protected static final int MERGE   = 1 << 3;

	protected DataOutputStream out;
	private final String[] format;
	private final int flags;

	protected Exporter(final int flags, final String... format) {
		this.flags = flags;
		this.format = format;
	}

	public final void output(final Drawing drawing, final OutputStream out) throws IOException {
		if ((flags & MERGE) != 0) {
			drawing.mergeLayers();
		}
		if ((flags & FLATTEN) != 0) {
			Drawing.flatness = Integer.getInteger("flatness", 1);
		}
		if ((flags & SHORTEN) != 0) {
			drawing.optimize();
		}
		this.out = new DataOutputStream(out);
		final Rectangle2D bounds = drawing.getBounds();
		final double ratio = 65535 / Math.max(bounds.getWidth(), bounds.getHeight());
		final int reverse = (flags & REVERSE) != 0 ? -1 : 1;
		final AffineTransform normalize = new AffineTransform(
				ratio, 0, 0, ratio * reverse, -bounds.getX() * ratio,
				((flags & REVERSE) * bounds.getHeight() + bounds.getY() * reverse) * ratio);

		writeHeader(bounds);
		for (final Drawing.Splash splash: drawing) {
			writeColor(splash.color);
			for (final PathIterator itr = splash.getPathIterator(normalize); !itr.isDone(); itr.next()) {
				writeSegment(itr.currentSegment(coords), coords);
			}
		}
		writeFooter();
	}

	protected abstract void writeHeader(final Rectangle2D bounds) throws IOException;
	protected abstract void writeFooter() throws IOException;
	protected abstract void writeColor(final Color color) throws IOException;

	protected void writeSegment(final int type, final double[] coords) throws IOException {
		int i = 0;
		for (final Character chr: format[type].toCharArray()) {
			out.writeBytes(chr == '%' ? Integer.toString((int) coords[i++])
				: Character.toString(chr));
		}
		out.write('\n');
	}

	protected final void writeFormat(final String format, final Object... args) throws IOException {
		out.writeBytes(String.format(format, args));
	}
}
