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
import java.awt.geom.PathIterator;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.io.PrintStream;
import org.apache.xmlgraphics.java2d.AbstractGraphics2D;
import org.apache.xmlgraphics.java2d.GraphicContext;

public class WriterGraphics2D extends AbstractGraphics2D {
	private final PrintStream out;
	private String[] format;
	private int flatness = -1;

	public WriterGraphics2D(PrintStream out, String[] format) {
		super(true);
		this.gc = new GraphicContext();
		this.out = out;
		this.format = format;
	}

	@Override
	public void draw(Shape s) {
		double[] coords = new double[6];
		PathIterator itr = flatness < 0 ? s.getPathIterator(null) : s.getPathIterator(null, flatness);

		for (; !itr.isDone(); itr.next()) {
			format(format[itr.currentSegment(coords)], coords);
		}
	}

	@Override
	public void fill(Shape s) {
		draw(s);
	}

	/** Replaces each '%' character in the input with an element from `args`. */
	private void format(String template, double[] args) {
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
	public void copyArea(int a, int b, int c, int d, int e, int f) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Graphics create() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void dispose() {
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
