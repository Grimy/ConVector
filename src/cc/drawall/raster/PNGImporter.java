package cc.drawall.raster;

import java.awt.image.BufferedImage;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import javax.imageio.ImageIO;

import cc.drawall.Importer;
import cc.drawall.Output;

public class PNGImporter implements Importer {

	private static final int HEIGHT = Integer.getInteger("shaky.height", 200);

	@Override
	public void process(final ReadableByteChannel input, final Output output) {
		System.out.println("Using PNGImporter!");
		BufferedImage img = null;
		try (InputStream stream = Channels.newInputStream(input)) {
			img = ImageIO.read(stream);
		} catch (final IOException e) {
			throw new IOError(e);
		}
		output.setSize(img.getWidth(), img.getHeight());
		int scale = 1 + (int) Math.sqrt(img.getWidth() * img.getHeight() / 77777.7);
		int dx = scale;
		int x = 0;
		for (int y = 0; y < img.getHeight(); y += scale) {
			output.writeSegment(y == 0 ? 0 : 1, x, y);
			for (x += dx; x >= 0 && x < img.getWidth(); x += dx) {
				final float height = scale * (float) darkness(img.getRGB(x, y));
				output.writeSegment(1, x - dx / 2d, y + height);
				output.writeSegment(1, x, y);
			}
			dx *= -1;
		}
	}

	/* HSP perceived brightness; see http://alienryderflex.com/hsp.html */
	private static double darkness(final int pixel) {
		// TODO: better color blending?
		// TODO: parameter instead of hardcoded .8?
		final int a = (pixel >> 24) & 0xFF;
		final int r = (pixel >> 16) & 0xFF;
		final int g = (pixel >>  8) & 0xFF;
		final int b = (pixel      ) & 0xFF;
		final double brightness = Math.sqrt(.299 * r * r + .587 * g * g + .114 * b * b);
		return (1 - brightness / 255.0) * (a / 255.0) * (HEIGHT / 255.0);
	}
}
