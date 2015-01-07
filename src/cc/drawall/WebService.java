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
		} catch (IOException e) {
			throw new IOError(e);
		}
	}
	
	WebService() throws IOException {
		serv = ServerSocketChannel.open();
		serv.bind(new InetSocketAddress(3434));
	}

	void loop() throws IOException {
		for (;;) {
			handle(serv.accept());
		}
	}

	private static ByteBuffer process(SocketChannel chan) {
		ByteBuffer result = ByteBuffer.allocate(10 << 20);
		Exporter gcode = new GCodeExporter();
		log.warning("Allocated " + result.capacity() + " bytes");
		Drawing drawing = new SVGImporter().process(chan).drawing;
		log.warning("Done importing");
		gcode.output(drawing, result);
		log.warning("Done exporting");
		result.flip();
		return result;
	}

	private static String readline(SocketChannel chan) throws IOException {
		final ByteBuffer buffer = ByteBuffer.allocate(1);
		String line = "";
		for (char c = '\0'; c != '\n';) {
			chan.read(buffer);
			buffer.flip();
			c = (char) buffer.get();
			line += c;
			buffer.clear();
		}
		return line.trim();
	}

	private static void handle(SocketChannel cli) throws IOException {
		String line = "Non Empty";
		int length = 0;
		log.warning("Received request");
		while (!line.isEmpty()) {
			line = readline(cli);
			log.warning("Read: " + line);
			if (line.startsWith("Content-Length: ")) {
				length = Integer.parseInt(line.replace("Content-Length: ", ""));
			}
		}
		log.warning("Length: " + length);
		ByteBuffer answer = length == 0 ? getHTML() : process(cli);
		log.warning("Done processing");
		while (answer.hasRemaining()) {
			answer.position(answer.position() + cli.write(answer));
		}
		log.warning("Done writing");
		cli.shutdownOutput();
	}
}

