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

package cc.drawall;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.logging.Logger;

/** Graphics context for all drawing operations done by Importer plugins.
  * Provides methods for setting graphical state, constructing paths and drawing them.
  * Graphical state includes the current transform matrix, clipping path,
  * font, stroke type, stroking color and filling color.
  * Path construction can be done using straight lines, Bézier curves, `closePath` instructions
  * and text.
  * Path drawing is either stroking, filling or both. */
public class Graphics {
	private static final Logger log = Logger.getLogger(Graphics.class.getName());

	private final AffineTransform ctm = new AffineTransform();
	private Area clippath = new Area(new Rectangle2D.Float(
				-Float.MAX_VALUE/2, -Float.MAX_VALUE/2,
				Float.MAX_VALUE, Float.MAX_VALUE));
	private final Path2D path = new Path2D.Float();
	private Point2D.Float smooth = null;

	private Color color = Color.BLACK;
	private Color fillColor = null;
	private Font font = null;
	private float fontSize = 1;
	private BasicStroke stroke = new BasicStroke(1, 0, 0, 10);
	private Drawing drawing = new Drawing();

	/* Saved graphical context. */
	private Graphics prev = null;

	Drawing getDrawing() {
		return drawing;
	}

	///////////////////////
	// Path construction //
	///////////////////////

	public void moveTo(final boolean relative, final float x, final float y) {
		final float[] points = {x, y};
		transform(relative, points);
		path.moveTo(points[0], points[1]);
	}

	public void lineTo(final boolean relative, final float... points) {
		transform(relative, points);
		if (points.length == 2) {
			path.lineTo(points[0], points[1]);
		} else if (points.length == 4) {
			path.quadTo(points[0], points[1], points[2], points[3]);
		} else if (points.length == 6) {
			path.curveTo(points[0], points[1], points[2], points[3], points[4], points[5]);
		} else {
			throw new IllegalArgumentException("Invalid length for point array: " + points.length);
		}
	}

	public void arcTo(final boolean relative, final Point2D radius, final float xAxisRotation,
			final boolean largeArcFlag, final boolean sweepFlag, final float... points) {
		transform(relative, points);
		ctm.deltaTransform(radius, radius);
		final Point2D p0 = path.getCurrentPoint();
		final double x0 = p0.getX();
		final double y0 = p0.getY();
		double rx = Math.abs(radius.getX());
		double ry = Math.abs(radius.getY());
		final double angle = Math.toRadians(xAxisRotation % 360.0);
		final double x2 = points[0];
		final double y2 = points[1];

		// Based on w3c’s SVG specification, Appendix F.6.5
		// Step 1 : Compute (x1, y1)
		final double dx2 = (x0 - x2) / 2.0;
		final double dy2 = (y0 - y2) / 2.0;
		final double cosAngle = Math.cos(angle);
		final double sinAngle = Math.sin(angle);
		final double x1 = cosAngle * dx2 + sinAngle * dy2;
		final double y1 = -sinAngle * dx2 + cosAngle * dy2;

		// Ensure radii are large enough
		final double radiiCheck = x1 * x1 / (rx * rx) + y1 * y1 / (ry * ry);
		if (radiiCheck > 1) {
			rx *= Math.sqrt(radiiCheck);
			ry *= Math.sqrt(radiiCheck);
		}

		// Step 2 : Compute (cx1, cy1)
		final double sq = (rx*rx * (ry*ry - y1*y1) - ry*ry * x1*x1) / (rx*rx * y1*y1 + ry*ry * x1*x1);
		final double coef = (largeArcFlag == sweepFlag ? -1 : 1) * Math.sqrt(Math.max(sq, 0));
		final double cx1 = coef * rx * y1 / ry;
		final double cy1 = coef * -ry * x1 / rx;

		// Step 3 : Compute (cx, cy) from (cx1, cy1)
		final double cx = (x0 + x2) / 2.0 + cosAngle * cx1 - sinAngle * cy1;
		final double cy = (y0 + y2) / 2.0 + sinAngle * cx1 + cosAngle * cy1;

		// Step 4 : Compute the angleStart and the angleExtent
		// TODO use atan2 or AffineTransform
		final double ux = (x1 - cx1) / rx;
		final double uy = (y1 - cy1) / ry;
		double n = Math.sqrt(ux * ux + uy * uy);
		final double angleStart = Math.toDegrees((uy < 0 ? -1d : 1d) * Math.acos(ux / n));
		final double vx = (-x1 - cx1) / rx;
		final double vy = (-y1 - cy1) / ry;
		n = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
		final double p = ux * vx + uy * vy;
		final double angleExtent = (Math.toDegrees((ux * vy < uy * vx ? -1.0 : 1.0)
				* Math.acos(p / n)) + (sweepFlag ? 360.0 : -360.0)) % 360.0;

		// We can now build the resulting Arc2D in double precision
		path.append(AffineTransform.getRotateInstance(angle, cx, cy).createTransformedShape(
			new Arc2D.Double(cx - rx, cy - ry, rx * 2.0, ry * 2.0,
				-angleStart, -angleExtent, Arc2D.OPEN)), true);
	}

	/** Closes the current path by drawing a straight line back to the coordinates of the
	  * last moveTo. If the path is already closed then this method has no effect. */
	public void closePath() {
		path.closePath();
	}

	/** Resets the path to its initial, empty state. */
	public void reset() {
		path.reset();
	}

	/** Appends the geometry of the specified Shape to the path.
	  * The winding rule of the specified Shape is ignored.
	  * @param shape whose geometry is appended to the path */
	public void append(final Shape shape) {
		path.append(shape, false);
	}

	/** Returns the coordinates most recently added to the end of the path as a Point2D. */
	public Point2D getCurrentPoint() {
		return path.getCurrentPoint();
	}

	/** Intersects the clipping area with the path.
	  * This sets the clipping area to the intersection of its current value with the area
	  * by the path, after closing it if necessary. */
	public void clip() {
		clippath.intersect(new Area(path));
	}

	public Rectangle2D getBounds() {
		return path.getBounds2D();
	}

	public void charpath(final String str) {
		assert font != null : "Undefined font";
		path.append(relativeTransform().createTransformedShape(font.createGlyphVector(
			new FontRenderContext(null, true, false), str).getOutline()), false);
	}

	private AffineTransform relativeTransform() {
		final Point2D point = path.getCurrentPoint();
		assert point != null : "No current point";
		return new AffineTransform(ctm.getScaleX(), ctm.getShearY(),
			ctm.getShearX(), ctm.getScaleY(), point.getX(), point.getY());
	}

	private void transform(final boolean relative, final float[] points) {
		log.finest(Arrays.toString(points));
		final int nbPoints = points.length / 2;
		(relative ? relativeTransform() : ctm).transform(points, 0, points, 0, nbPoints);
		final Point2D point = nbPoints > 1 && smooth != null ? smooth : path.getCurrentPoint();
		if (point != null) {
			points[0] = Float.isNaN(points[0]) ? (float) point.getX() : points[0];
			points[1] = Float.isNaN(points[1]) ? (float) point.getY() : points[1];
		}
		smooth = nbPoints == 1 ? null : new Point2D.Float(
				2 * points[points.length - 2] - points[points.length - 4],
				2 * points[points.length - 1] - points[points.length - 3]);
	}

	////////////////////////
	// Drawing operations //
	////////////////////////

	public void draw() {
		if (fillColor != null) {
			path.closePath();
			paintArea(fillColor, new Area(path));
		}
		if (color != null) {
			try {
				final Shape inverse = ctm.createInverse().createTransformedShape(path);
				final Shape stroked = stroke.createStrokedShape(inverse);
				paintArea(color, new Area(ctm.createTransformedShape(stroked)));
			} catch (final NoninvertibleTransformException e) {
				// Non-invertible transforms squash any shape to empty areas
				log.finer(e.toString());
			}
		}
		path.reset();
	}

	public void fill(final int windingRule) {
		path.closePath();
		path.setWindingRule(windingRule);
		paintArea(color, new Area(path));
		path.reset();
	}

	public void drawString(final String str) {
		charpath(str);
		fill(Path2D.WIND_EVEN_ODD);
	}

	private void paintArea(final Color color, final Area area) {
		area.intersect(clippath);
		drawing.paint(color, area);
	}

	///////////////////////
	// Graphics settings //
	///////////////////////

	public void setStrokeWidth(final float width) {
		setStroke("width", width);
	}

	public void setStrokeDash(final float[] dash, final float phase) {
		setStroke("dash", dash);
		setStroke("phase", phase);
	}

	public void setLineCap(final int cap) {
		setStroke("cap", cap);
	}

	public void setLineJoin(final int join) {
		setStroke("join", join);
	}

	public void setMiterLimit(final float limit) {
		setStroke("miterlimit", limit);
	}

	private void setStroke(final String fieldName, final Object value) {
		try {
			final Field field = BasicStroke.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(stroke, value);
		} catch (final IllegalAccessException | NoSuchFieldException e) {
			throw new AssertionError("No such field: " + fieldName, e);
		}
	}

	public void setColor(final Color color) {
		this.color = color;
	}

	public void setFillColor(final Color color) {
		this.fillColor = color;
	}

	public AffineTransform getTransform() {
		return ctm;
	}

	public Area getClip() {
		return clippath;
	}

	public void setFont(final String fontDescriptor) {
		final String fontName = fontDescriptor.split(" ")[0];
		final GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		if (!Arrays.asList(env.getAvailableFontFamilyNames()).contains(fontName)) {
			try {
				log.info("Loading font " + fontDescriptor);
				env.registerFont(Font.createFont(Font.TYPE1_FONT,
					getClass().getResourceAsStream("/fonts/" + fontName)));
			} catch (final IOException | FontFormatException e) {
				// Fallback to another font
				log.warning("Cannot load font " + fontName + ": " + e);
			}
		}
		// TODO: revert font only when necessary
		font = new Font(fontDescriptor, 0, (int) fontSize)
			.deriveFont(AffineTransform.getScaleInstance(1, -1));
	}

	public void setFontSize(final float fontSize) {
		this.fontSize = fontSize;
	}

	public void save() {
		final Graphics copy = new Graphics();
		copy.copy(this);
		prev = copy;
	}

	public void restore() {
		copy(prev);
	}

	private void copy(final Graphics that) {
		this.ctm.setTransform(that.ctm);
		this.clippath = (Area) that.clippath.clone();
		this.color = that.color;
		this.fillColor = that.fillColor;
		this.font = that.font;
		this.stroke = new BasicStroke(stroke.getLineWidth(), stroke.getEndCap(),
				stroke.getLineJoin(), stroke.getMiterLimit(), stroke.getDashArray(),
				stroke.getDashPhase());
		this.drawing = that.drawing;
		this.prev = that.prev;
		this.path.reset();
		this.path.append(that.path, false);
	}
}
