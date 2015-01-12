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
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

class WebService {
	private static final Logger log = Logger.getLogger(WebService.class.getName());

	private final ServerSocketChannel serv;

	WebService(final int port) throws IOException {
		serv = ServerSocketChannel.open();
		serv.bind(new InetSocketAddress(port));
		log.info("Listening on port " + port);
	}

	private static ByteBuffer getHTML() {
		try (final RandomAccessFile file = new RandomAccessFile(
					WebService.class.getResource("/convector.html").getFile(), "rw");
				final FileChannel chan = file.getChannel()) {
			return chan.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
		} catch (final IOException e) {
			throw new IOError(e);
		}
	}
	
	private static ByteBuffer process(final HTTPChannel chan) {
		final String[] filetypes = chan.url.split("/");
		if (filetypes.length < 3) {
			return getHTML();
		}
		final ByteBuffer result = ByteBuffer.allocate(10 << 20);
		final Drawing drawing = Importer.importStream(chan, filetypes[1]);
		Exporter.exportStream(result, filetypes[2], drawing);
		result.flip();
		return result;
	}

	private static void send(final ByteBuffer reply,
			final SocketChannel client) throws IOException {
		while (reply.hasRemaining()) {
			reply.position(reply.position() + client.write(reply));
		}
	}

	void loop() {
		for (;;) {
			try (final SocketChannel client = serv.accept();
				final HTTPChannel query = new HTTPChannel(client)) {
				log.info("Received query: " + query.url + " from " + client.getRemoteAddress());
				final ByteBuffer reply = process(query);
				send(reply, client);
				log.info("Successfully sent " + reply.position() + "B of reply");
			} catch (final IOException e) {
				log.severe("WebService error: " + e);
			}
		}
	}
}
