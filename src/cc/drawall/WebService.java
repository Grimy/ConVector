/* This file is part of DraWall.
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
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

class WebService implements Runnable {
	private static final Logger log = Logger.getLogger(WebService.class.getName());
	private static final ByteBuffer html = ByteBuffer.allocate(4096);

	static {
		try (final InputStream in = WebService.class.getResourceAsStream("/convector.html");
			final ReadableByteChannel chan = Channels.newChannel(in)) {
			chan.read(html);
			html.flip();
		} catch (final IOException e) {
			throw new IOError(e);
		}
	}

	private SocketChannel client;

	WebService(final SocketChannel client) {
		this.client = client;
	}

	@Override
	public void run() {
		try (final HTTPChannel query = new HTTPChannel(client)) {
			log.info("Received query: " + query.url + " from " + client.getRemoteAddress());
			final ByteBuffer reply = process(query);
			send(reply);
			log.info("Successfully sent " + reply.position() + "B of reply");
			client.shutdownOutput();
		} catch (final IOException e) {
			throw new IOError(e);
		}
	}

	private static ByteBuffer process(final HTTPChannel chan) {
		final String[] filetypes = chan.url.split("/");
		if (filetypes.length < 3) {
			html.position(0);
			return html;
		}
		final ByteBuffer result = ByteBuffer.allocate(10 << 20);
		final Drawing drawing = ConVector.importStream(chan, filetypes[1]);
		ConVector.exportStream(result, filetypes[2], drawing);
		result.flip();
		return result;
	}

	private void send(final ByteBuffer reply) throws IOException {
		while (reply.hasRemaining()) {
			reply.position(reply.position() + client.write(reply));
		}
	}

	static void loop(final int port) {
		try (final ServerSocketChannel serv = ServerSocketChannel.open()) {
			serv.bind(new InetSocketAddress(port));
			log.info("Listening on port " + port);
			for (;;) {
				new Thread(new WebService(serv.accept())).start();
			}
		} catch (final IOException e) {
			throw new IOError(e);
		}
	}
}
