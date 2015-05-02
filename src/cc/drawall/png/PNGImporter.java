package cc.drawall.png;

import cc.drawall.Importer;
import cc.drawall.Canvas;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Channels;
import java.io.InputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class PNGImporter implements Importer {

	private Canvas g = new Canvas();

	@Override
	public Canvas process(final ReadableByteChannel input) {
		InputStream stream = Channels.newInputStream(input);
		BufferedImage img = null;
	       	try {
		       img = ImageIO.read(stream);
		} catch (IOException e) {
			assert false; // TODO
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
		return g.stroke();
	}

	private void drawPixel(int rgb, int dx) {
		int red = rgb >> 16;
		int green = (rgb >> 8) & 255;
		int blue = rgb & 255;
		double brightness = red * .2126 + green * .7152 + blue * .0722;
		if (brightness < .5) {
			g.lineTo(dx * .5f, .5f);
			g.lineTo(dx * .5f, -.5f);
		} else {
			g.lineTo(dx, 0);
		}
	}
}
