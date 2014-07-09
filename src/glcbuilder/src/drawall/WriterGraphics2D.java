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
 * © 2014 Victor Adam
 */

package drawall;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import org.apache.xmlgraphics.java2d.AbstractGraphics2D;
import org.apache.xmlgraphics.java2d.GraphicContext;

public abstract class WriterGraphics2D extends AbstractGraphics2D {

	private Stack<Area> areas = new Stack<>();
	private Stack<Color> colors = new Stack<>();
	protected int flatness = -1;
	protected double[] coords = new double[6]; // buffer
	protected Map<Color, Area> colorMap = new HashMap<>();

	public WriterGraphics2D() {
		super(true);
		this.gc = new GraphicContext();
	}

	public double computeSurface(Area a) {
		PathIterator i = a.getPathIterator(null, 0);
		double surface = 0.0;
		double x = 0.0, y = 0.0, startX = 0.0, startY = 0.0, prevX, prevY;

		for (; !i.isDone(); i.next()) {
			prevX = x;
			prevY = y;
			int segType = i.currentSegment(coords);
			if (segType == PathIterator.SEG_MOVETO) {
				x = startX = coords[0];
				y = startY = coords[1];
			} else {
				boolean close = segType == PathIterator.SEG_CLOSE;
				x = close ? startX : coords[0];
				y = close ? startY : coords[1];
				surface += prevX * y - x * prevY;
			}
		}
		return Math.abs(surface) / 2;
	}

	@Override
	public void draw(Shape s) {
		fill(gc.getStroke().createStrokedShape(s));
	}

	@Override
	public void fill(Shape s) {
		Area a = new Area(s);
		if (gc.getClip() != null) {
			a.intersect(new Area(gc.getClip()));
		}
		a.transform(gc.getTransform());
		areas.push(a);
		colors.push(gc.getColor());
	}

	// TODO: move this to an util class
	/** Replaces each '%' character in the input with an element from `args`. */
	public static void format(PrintStream out, String template, double[] args) {
		// Irritatingly, String.format treats a double[] as a single Object.
		// Converting to a Double[] works, but is cumbersome and terrible for perf.
		// That’s why we have our own implementation of this.
		int i = 0;
		for (char c: template.toCharArray()) {
			if (c == '%') {
				out.print((int) args[i++]);
			} else {
				out.print(c);
			}
		}
		out.println();
	}

	public void done(PrintStream out) {
		Area mask = new Area();

		while (!areas.isEmpty()) {
			Area a = areas.pop();
			Color color = colors.pop();

			a.subtract(mask); // XXX: this increases the filesize by >50%
			if (a.isEmpty()) {
				continue;
			}
			mask.add(a);

			// Rectangle2D r = a.getBounds2D();
			// double w = r.getWidth(), h = r.getHeight();
			// double ratio = computeSurface(a) * 2 / (w * w + h * h);
			// if (ratio > .2 && computeSurface(a) > 900) {
			// System.err.println(ratio + ", " + computeSurface(a));
			// continue;
			// }
			// color = ratio < .2 ? Color.RED : ratio < .4 ? Color.BLUE : new Color(rng.nextInt());

			if (colorMap.containsKey(color)) {
				colorMap.get(color).add(a);
			} else {
				colorMap.put(color, a);
			}
		}

		Rectangle bounds = mask.getBounds();
		double ratio = 65535.0 / Math.max(bounds.width, bounds.height);
		AffineTransform normalize = new AffineTransform(ratio, 0, 0, ratio, -bounds.x * ratio, -bounds.y * ratio);

		output(normalize, out);
	}

	protected abstract void output(AffineTransform transform, PrintStream out);

	/******************************************\
	|* ONLY BOILERPLATE CODE BEYOND THIS LINE *|
	\******************************************/

	@Override
	public void dispose() {
		/* No resources to free */
	}

	@Override
	public void copyArea(int x, int y, int width, int height, int dx, int dy) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Graphics create() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean drawImage(Image img, int x, int y, ImageObserver obs) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver obs) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void drawRenderableImage(RenderableImage image, AffineTransform transform) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void drawRenderedImage(RenderedImage image, AffineTransform transform) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void drawString(String str, float x, float y) {
		throw new UnsupportedOperationException();
	}

	@Override
	public GraphicsConfiguration getDeviceConfiguration() {
		throw new UnsupportedOperationException();
	}

	@Override
	public FontMetrics getFontMetrics(Font font) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setXORMode(Color color) {
		throw new UnsupportedOperationException();
	}
}
