package cc.drawall.png;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import javax.imageio.ImageIO;

import cc.drawall.Canvas;
import cc.drawall.Importer;
import cc.drawall.Output;

public class PNGImporter implements Importer {

	private Canvas g;

	@Override
	public void process(final ReadableByteChannel input, final Output output) {
		g = new Canvas(output);
		BufferedImage img = null;
		try (InputStream stream = Channels.newInputStream(input)) {
			img = ImageIO.read(stream);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
		g.setSize(img.getWidth(), img.getHeight());
		g.setRelative(false).moveTo(0, 0);
		g.setRelative(true);
		int dx = 1;
		int x = 0;
		for (int y = 0; y < img.getHeight(); ++y) {
			for (x += dx; x % img.getWidth() != 0; x += dx) {
				drawPixel(img.getRGB(x, y), dx);
			}
			g.lineTo(0, 1);
			dx *= -1;
		}
		g.stroke();
	}

	private void drawPixel(int rgb, int dx) {
		double alpha = ((rgb >> 24) & 255) / 256d;
		double red =   ((rgb >> 16) & 255) / 256d;
		double green = ((rgb >> 8)  & 255) / 256d;
		double blue =  ((rgb)       & 255) / 256d;
		// HSP perceived brightness; see http://alienryderflex.com/hsp.html
		final double brightness = Math.sqrt(.299 * red * red + .587 * green * green + .114 * blue * blue);
		final float height = (float) ((1 - brightness) * alpha * .8);
		g.lineTo(dx * .5f, height);
		g.lineTo(dx * .5f, -height);
	}
}
