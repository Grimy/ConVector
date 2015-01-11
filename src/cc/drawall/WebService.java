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
		final Drawing drawing = PluginOverseer.importStream(chan, filetypes[1]);
		PluginOverseer.exportStream(result, filetypes[2], drawing);
		result.flip();
		return result;
	}

	private static void send(ByteBuffer reply, SocketChannel client) throws IOException {
		while (reply.hasRemaining()) {
			reply.position(reply.position() + client.write(reply));
		}
	}

	WebService() throws IOException {
		int port = 3434;
		serv = ServerSocketChannel.open();
		serv.bind(new InetSocketAddress(port));
		log.info("Listening on port " + port);
	}

	void loop() {
		for (;;) {
			try (final SocketChannel client = serv.accept();
				final HTTPChannel query = new HTTPChannel(client)) {
				log.info("Received query: " + query.url + " from " + client.getRemoteAddress());
				ByteBuffer reply = process(query);
				send(reply, client);
				log.info("Successfully sent " + reply.position() + "B of reply");
			} catch (IOException e) {
				log.severe("WebService error: " + e);
			}
		}
	}
}
