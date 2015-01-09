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

import cc.drawall.gcode.GCodeExporter;
import cc.drawall.svg.SVGImporter;

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
	
	private static ByteBuffer process(final SocketChannel chan) {
		final ByteBuffer result = ByteBuffer.allocate(10 << 20);
		final Exporter gcode = new GCodeExporter();
		final Drawing drawing = new SVGImporter().process(chan).drawing;
		gcode.output(drawing, result);
		result.flip();
		return result;
	}

	private static String readline(final SocketChannel chan) throws IOException {
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

	private static void handle(final SocketChannel cli) throws IOException {
		String line = "Non Empty";
		int length = 0;
		log.info("Received request");
		while (!line.isEmpty()) {
			line = readline(cli);
			if (line.startsWith("Content-Length: ")) {
				length = Integer.parseInt(line.replace("Content-Length: ", ""));
			}
		}
		log.info("Length: " + length);
		final ByteBuffer answer = length == 0 ? getHTML() : process(cli);
		while (answer.hasRemaining()) {
			answer.position(answer.position() + cli.write(answer));
		}
		log.info("Done writing");
		cli.shutdownOutput();
	}

	WebService() throws IOException {
		serv = ServerSocketChannel.open();
		serv.bind(new InetSocketAddress(80));
	}

	void loop() throws IOException {
		for (;;) {
			handle(serv.accept());
		}
	}
}
