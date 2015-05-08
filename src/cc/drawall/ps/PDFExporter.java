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

package cc.drawall.ps;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import cc.drawall.Exporter;

/** Outputs a vector as a PDF. */
public class PDFExporter extends Exporter {

	private final List<Integer> xref = new ArrayList<>(4);
	private int bytes = 0;

	public PDFExporter() {
		/* REVERSE | FLATTEN */
		super("% % m", "% % l", "", "% % % % % % c", "h");
	}

	private ByteBuffer write(final String format, final Object... args) {
		final ByteBuffer result = format(format, args);
		bytes += result.flip().position();
		return result;
	}

	private ByteBuffer obj(final String content, final Object... args) {
		xref.add(bytes);
		return write(xref.size() + " 0 obj" + content + '\n', args);
	}

	@Override
	protected ByteBuffer header(final double width, final double height, final double ratio) {
		return ByteBuffer.allocate(256)
			.put(write("%%PDF-1\n%% Painstakingly %s ConVector\n", COMMENT))
			.put(obj("<</Pages 1 0 R/Kids[2 0 R]/Count 1>>endobj"))
			.put(obj("<</Contents 3 0 R/MediaBox[0 0 %.3f %.3f]>>endobj", width, height))
			.put(obj("<</Length 4 0 R>>stream"))
			.put(write("%f 0 0 %f 0 0 cm\n", ratio, ratio));
	}

	@Override
	protected ByteBuffer color(final double red, final double green, final double blue) {
		return write("h f %.3f %.3f %.3f rg\n", red, green, blue);
	}

	@Override
	protected ByteBuffer footer() {
		final ByteBuffer result = ByteBuffer.allocate(256)
			.put(write("f endstream\nendobj\n"))
			.put(obj(" %d endobj", bytes - xref.get(2) - 48));
		final int startxref = bytes;
		result.put(write("xref\n1 " + xref.size() + "\n"));
		for (final int pos: xref) {
			write("%010d 00000  n\n", pos);
		}
		return result
			.put(write("trailer<</Size %d/Root 1 0 R>>", xref.size() + 1))
			.put(write("startxref " + startxref + "\n%%%%EOF"));
	}
}
