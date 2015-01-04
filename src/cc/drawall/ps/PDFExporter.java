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
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cc.drawall.Exporter;

/** Outputs a vector as a PDF. */
public class PDFExporter extends Exporter {

	private final List<Integer> xref = new ArrayList<>(4);

	/** Constructor. */
	public PDFExporter() {
		super(REVERSE, "% % m", "% % l", null, "% % % % % % c", "h");
	}

	@Override
	protected void writeHeader(final Rectangle2D bounds) throws IOException {
		write("%%PDF-1\n");
		writeObj("<</Pages 1 0 R/Kids[2 0 R]/Count 1>>");
		writeObj("<</Contents 3 0 R/MediaBox[0 0 " + (int) bounds.getWidth() + " "
				+ (int) bounds.getHeight() + "]>>");
		writeObj("<</Length 4 0 R>>stream\n%%");
		final double ratio = Math.max(bounds.getWidth(), bounds.getHeight()) / 65535;
		write("%f 0 0 %f 0 0 cm\n", ratio, ratio);
	}

	@Override
	protected void writeColor(final Color color) throws IOException {
		final float[] rgb = color.getRGBColorComponents(null);
		write("h f %.3f %.3f %.3f rg%n", rgb[0], rgb[1], rgb[2]);
	}

	@Override
	protected void writeFooter() throws IOException {
		write("f endstream\nendobj\n");
		writeObj(" " + (bytesWritten() - xref.get(2) - 48) + " ");
		final int startxref = bytesWritten();
		write("xref\n1 " + xref.size() + "\n");
		for (final int pos: xref) {
			write(String.format("%010d 00000  n%n", pos));
		}
		write("trailer<</Size %d/Root 1 0 R>>", xref.size() + 1);
		write("startxref " + startxref + "\n%%%%EOF");
	}

	private void writeObj(final String content) throws IOException {
		xref.add(bytesWritten());
		write(xref.size() + " 0 obj" + content + "endobj\n");
	}
}
