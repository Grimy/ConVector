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

package cc.drawall;

import java.io.IOError;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/** The base class for all Exporter plugins.
  * Provides a common template for all output filetypes. Abstract methods should be overriden
  * to implement the details relevant to a particular filetype. */
class SimpleOutput implements Output {

	final WritableByteChannel out;
	final Exporter exporter;

	SimpleOutput(final WritableByteChannel out, final Exporter exporter) {
		this.out = out;
		this.exporter = exporter;
	}

	private void write(final ByteBuffer buf) {
		try {
			out.write(buf);
		} catch (final IOException e) {
			throw new IOError(e);
		}
	}

	@Override
	public void setSize(final double width, final double height) {
		exporter.ratio = Math.max(width, height) / 65535;
		write(exporter.header(width, height, exporter.ratio));
	}

	@Override
	public void writeColor(final double red, final double green, final double blue) {
		write(exporter.color(red, green, blue));
	}

	@Override
	public void writeSegment(final int type, final double... coords) {
		write(exporter.segment(type, coords));
	}

	@Override
	public void writeFooter() {
		write(exporter.footer());
	}
}
