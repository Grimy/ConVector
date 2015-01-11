package cc.drawall;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

class HTTPChannel implements ReadableByteChannel {
	private SocketChannel chan;
	private int remaining;
	private static final Logger log = Logger.getLogger(WebService.class.getName());
	String url;
	String host;

	HTTPChannel(SocketChannel chan) throws IOException {
		this.chan = chan;
		parseHeaders();
	}

	private String readline() throws IOException {
		final ByteBuffer buffer = ByteBuffer.allocate(1);
		final StringBuilder line = new StringBuilder();
		for (char c = '\0'; c != '\n';) {
			chan.read(buffer);
			buffer.flip();
			c = (char) buffer.get();
			line.append(c);
			buffer.clear();
		}
		return line.toString().trim();
	}

	void parseHeaders() throws IOException {
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

	@Override
	public int read(ByteBuffer dest) throws IOException {
		if (remaining <= 0) {
			return -1;
		}
		int read = chan.read(dest);
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
