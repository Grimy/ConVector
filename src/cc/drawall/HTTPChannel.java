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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

class HTTPChannel implements ReadableByteChannel {
	private final SocketChannel chan;
	private int remaining;
	private static final Logger log = Logger.getLogger(HTTPChannel.class.getName());
	final String url;

	HTTPChannel(final SocketChannel chan) throws IOException {
		this.chan = chan;
		url = readline().split(" ")[1];
		String line = ".";
		while (!line.isEmpty()) {
			line = readline();
			log.finest("Received header: " + line);
			if (line.startsWith("Content-Length: ")) {
				remaining = Integer.parseInt(line.replace("Content-Length: ", ""));
			}
		}
	}

	private String readline() throws IOException {
		final ByteBuffer buffer = ByteBuffer.allocate(1);
		final StringBuilder line = new StringBuilder();
		char c = '\0';
		while (c != '\n') {
			chan.read(buffer);
			buffer.flip();
			c = (char) buffer.get();
			line.append(c);
			buffer.clear();
		}
		return line.toString().trim();
	}

	@Override
	public int read(final ByteBuffer dest) throws IOException {
		if (remaining <= 0) {
			return -1;
		}
		final int read = chan.read(dest);
		remaining -= read;
		return read;
	}

	@Override
	public boolean isOpen() {
		return chan.isOpen();
	}

	@Override
	public void close() throws IOException {
		// Do not close the underlying channel.
		// This is a protection against SAX’s abusive close requests.
	}
}
