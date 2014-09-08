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
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import org.apache.xmlgraphics.java2d.AbstractGraphics2D;
import org.apache.xmlgraphics.java2d.GraphicContext;

public class WriterGraphics2D extends AbstractGraphics2D {

	private Stack<Area> areas = new Stack<>();
	private Stack<Color> colors = new Stack<>();
	private Area mask;

	protected WriterGraphics2D(GraphicContext gc) {
		super(true);
		this.gc = gc;
	}

	public WriterGraphics2D() {
		this(new GraphicContext());
	}

	@Override
	public Graphics create() {
		return new WriterGraphics2D((GraphicContext) gc.clone());
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

	// Clip, merge, split, normalize, optimize
	public Map<Color, Area> getColorMap() {
		mask = new Area();
		Map<Color, Area> colorMap = new HashMap<>();

		while (!areas.isEmpty()) {
			System.out.println(areas.size());
			Area a = areas.pop();
			Color color = colors.pop();
			a.subtract(mask); // XXX: this increases the filesize by >50%
			if (a.isEmpty()) {
				continue;
			}
			mask.add(a);

			// Rectangle2D r = a.getBounds2D();
			// double w = r.getWidth(), h = r.getHeight();
			// double ratio = computeSurface(a) * 2 / (w * w );
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

		return colorMap;
	}

	public AffineTransform getNormalizingTransform() {
		Rectangle bounds = mask.getBounds();
		double ratio = 65535.0 / Math.max(bounds.width, bounds.height);
		return new AffineTransform(ratio, 0, 0, ratio, -bounds.x * ratio, -bounds.y * ratio);
	}

	/******************************************\
	|* ONLY BOILERPLATE CODE BEYOND THIS LINE *|
	\******************************************/

	@Override
	public void dispose() {
		/* No resources to free */
	}

	@Override
	public void drawString(String str, float x, float y) {
		drawGlyphVector(getFont().createGlyphVector(getFontRenderContext(), str), x, y);
	}

	@Override
	public void copyArea(int x, int y, int width, int height, int dx, int dy) {
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
