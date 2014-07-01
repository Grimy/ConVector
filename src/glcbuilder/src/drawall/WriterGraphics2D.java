/*
 * This file is part of DraWall.
 * DraWall is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * DraWall is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with DraWall. If not, see <http://www.gnu.org/licenses/>.
 * Copyright (c) 2012-2014 Nathanaël Jourdane.
 */

package drawall;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
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

/* A Graphics2D object that writes to a stream. */
public class WriterGraphics2D extends AbstractGraphics2D {

	private final PrintStream out;

	private int flatness = -1;
	private double[] coords = new double[6]; // buffer
	private Stack<Area> areas = new Stack<>();
	private Stack<Color> colors = new Stack<>();
	private Map<Color, Area> colorMap = new HashMap<>();

	public static final String[] GLC = {
		"G00 X% Y%", "G01 X% Y%", "G5.1 I% J% X% Y%", "G05 I% J% P% Q% X% Y%", "",
		"", "M30"
	};
	public static final String[] SVG = {
		"M %,%", "L %,%", "Q %,% %,%", "C %,% %,% %,%", "z",
		"<?xml version='1.0' encoding='UTF-8' standalone='no'?>\n" +
			"<!DOCTYPE svg PUBLIC '-//W3C//DTD SVG 1.0//EN' 'http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd'>\n" +
			"<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 300 300'>",
		"</svg>"
	};
	private String[] format = SVG;

	public WriterGraphics2D(PrintStream out) {
		super(true);
		this.gc = new GraphicContext();
		this.out = out;

		out.println(format[5]);
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

	private static int getPathLength(Area a) {
		int result = 0;
		for (PathIterator i = a.getPathIterator(null); !i.isDone(); i.next()) {
			result++;
		}
		return result;
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

	protected void paintSVG(Area a, Color color) {
		// Random rng = new Random();

		PathIterator itr = flatness < 0 ? a.getPathIterator(null) : a.getPathIterator(null, flatness);

		out.format("<path style='fill:#%06x;stroke:none' d='", color.getRGB() & 0xFFFFFF);
		// out.format("<path style='fill:#%06x;stroke:none;opacity:0.5' d='", rng.nextInt() & 0xFFFFFF);
		for (; !itr.isDone(); itr.next()) {
			format(format[itr.currentSegment(coords)], coords);
		}
		out.print("'/>");
	}

	// protected void paintGCode(ColoredArea a) {
	// }

	/** Replaces each '%' character in the input with an element from `args`. */
	protected void format(String template, double[] args) {
		// Irritatingly, String.format treats a double[] as a single Object.
		// Converting to a Double[] works, but is cumbersome and terrible for perf.
		// That’s why we have our own implementation of this.
		int i = 0;
		for (char c: template.toCharArray()) {
			if (c == '%') {
				out.format("%.3f", args[i++]);
			} else {
				out.print(c);
			}
		}
		out.println();
	}

	@Override
	public void dispose() {
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

		for (Color color: colorMap.keySet()) {
			paintSVG(colorMap.get(color), color);
		}

		out.println(format[6]);
		out.close();
	}

	@Override
	public void copyArea(int a, int b, int c, int d, int e, int f) {
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
