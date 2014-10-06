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

public class WriterGraphics2D {
	private static final Logger log = Logger.getLogger(WriterGraphics2D.class.getName());

	static {
		try {
			// TODO generalize this for other fonts
			Font font = Font.createFont(Font.TYPE1_FONT,
					PSImporter.class.getResourceAsStream("fonts/Palatino"));
			GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
		} catch (IOException | FontFormatException e) {
			// Fallback to another font
			log.warning("Cannot load font Palatino: " + e);
		}
	}

	private final AffineTransform ctm = new AffineTransform();
	private Area clippath = new Area(new Rectangle2D.Float(Float.MIN_VALUE, Float.MIN_VALUE,
				Float.MAX_VALUE, Float.MAX_VALUE));
	private final Path2D path = new Path2D.Float();

	private Color color = Color.BLACK;
	private Color fillColor = null;
	private Font font = null;
	private BasicStroke stroke = new BasicStroke(1, 0, 0, 10);
	private Drawing drawing = new Drawing();

	/** Saved graphical context. */
	private WriterGraphics2D prev = null;

	public Drawing getDrawing() {
		return drawing;
	}

	///////////////////////
	// Path construction //
	///////////////////////

	public void moveTo(boolean relative, final float... points) {
		transform(relative, points, 1);
		path.moveTo(points[0], points[1]);
	}

	public void lineTo(boolean relative, final float... points) {
		transform(relative, points, 1);
		path.lineTo(points[0], points[1]);
	}

	public void quadTo(boolean relative, final float... points) {
		transform(relative, points, 2);
		path.quadTo(points[0], points[1], points[2], points[3]);
	}

	public void curveTo(boolean relative, final float... points) {
		transform(relative, points, 3);
		path.curveTo(points[0], points[1], points[2], points[3], points[4], points[5]);
	}

	public void arcTo(boolean relative, Point2D radius, float xAxisRotation,
			boolean largeArc, boolean sweep, final float... points) {
		transform(relative, points, 1);
		ctm.deltaTransform(radius, radius);
		Arc2D arc = computeArc(
			getCurrentPoint().getX(), getCurrentPoint().getY(),
			radius.getX(), radius.getY(),
			xAxisRotation, largeArc, sweep,
			points[0], points[1]);
		AffineTransform rotate = AffineTransform.getRotateInstance(
				Math.toRadians(xAxisRotation), arc.getCenterX(), arc.getCenterY());
		path.append(rotate.createTransformedShape(arc), true);
	}

	public void closePath() {
		path.closePath();
	}

	public void reset() {
		path.reset();
	}

	public void append(final Shape shape) {
		path.append(shape, false);
	}

	public Point2D getCurrentPoint() {
		return path.getCurrentPoint();
	}

	public void clip() {
		clippath.intersect(new Area(path));
	}

	public Rectangle2D getBounds() {
		return path.getBounds2D();
	}

	public void charpath(final String str) {
		assert font != null : "Undefined font";
		Point2D p = getCurrentPoint();
		assert p != null : "No current point";
		path.append(font.createGlyphVector(new FontRenderContext(ctm, true, false), str)
			.getOutline((float) p.getX(), (float) p.getY()), false);
	}

	private void transform(boolean relative, final float[] points, final int nbPoints) {
		assert points.length == 2 * nbPoints : "Expected: " + 2 * nbPoints + "Got: " + points.length;
		log.finer(Arrays.toString(points));
		Point2D p = path.getCurrentPoint();
		(relative ? new AffineTransform(
				ctm.getScaleX(), ctm.getShearY(), ctm.getShearX(), ctm.getScaleY(),
				p.getX(), p.getY()) : ctm).transform(points, 0, points, 0, nbPoints);
		if (p != null) {
			// TODO this is GCode specific; move it away?
			points[0] = Float.isNaN(points[0]) ? (float) p.getX() : points[0];
			points[1] = Float.isNaN(points[1]) ? (float) p.getY() : points[1];
		}
	}

	////////////////////////
	// Drawing operations //
	////////////////////////

	public void draw() {
		log.warning(fillColor + " ; " + color);
		if (fillColor != null) {
			path.closePath();
			paintArea(fillColor, new Area(path));
		}
		if (color != null) {
			try {
				final Shape inverse = ctm.createInverse().createTransformedShape(path);
				final Shape stroked = stroke.createStrokedShape(inverse);
				paintArea(color, new Area(ctm.createTransformedShape(stroked)));
			} catch (NoninvertibleTransformException e) {
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

	private void paintArea(final Color color, final Area area) {
		area.intersect(clippath);
		drawing.paint(color, area);
	}

	public void drawString(final String str) {
		// TODO: preserve previous path?
		charpath(str);
		fill(Path2D.WIND_EVEN_ODD);
	}

	///////////////////////
	// Graphics settings //
	///////////////////////

	public void setStrokeWidth(final float width) {
		setStroke("width", width);
	}

	public void setStrokeCap(final int cap) {
		setStroke("cap", cap);
	}

	public void setStrokeJoin(final int join) {
		setStroke("join", join);
	}

	public void setStrokeMiterLimit(final float limit) {
		setStroke("miterlimit", limit);
	}

	public void setStrokeDash(final float[] dash, final float phase) {
		setStroke("dash", dash);
		setStroke("phase", phase);
	}

	private void setStroke(final String fieldName, final Object value) {
		try {
			final Field field = BasicStroke.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(stroke, value);
		} catch (IllegalAccessException | NoSuchFieldException e) {
			throw new IllegalArgumentException("No such field: " + fieldName, e);
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

	public void setFont(final Font font) {
		this.font = font;
	}

	public void save() {
		final WriterGraphics2D copy = new WriterGraphics2D();
		copy.copy(this);
		prev = copy;
	}

	public void restore() {
		copy(prev);
	}

	private void copy(final WriterGraphics2D that) {
		this.ctm.setTransform(that.ctm);
		this.clippath.intersect(that.clippath);
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

	/**
	 * Copy-pasted from http://svn.apache.org/repos/asf/xmlgraphics/batik/
	 * branches/svg11/sources/org/apache/batik/ext/awt/geom/ExtendedGeneralPath.java
	 * This constructs an unrotated Arc2D from the SVG specification of an
	 * Elliptical arc.  To get the final arc you need to apply a rotation
	 * transform such as:
	 *
	 * AffineTransform.getRotateInstance
	 *     (angle, arc.getX()+arc.getWidth()/2, arc.getY()+arc.getHeight()/2);
	 */
	public static Arc2D computeArc(double x0, double y0,
								   double $rx, double $ry,
								   double $angle,
								   boolean largeArcFlag,
								   boolean sweepFlag,
								   double x, double y) {
		double rx = $rx;
		double ry = $ry;
		double angle = $angle;
		//
		// Elliptical arc implementation based on the SVG specification notes
		//

		// Compute the half distance between the current and the final point
		final double dx2 = (x0 - x) / 2.0;
		final double dy2 = (y0 - y) / 2.0;
		// Convert angle from degrees to radians
		angle = Math.toRadians(angle % 360.0);
		final double cosAngle = Math.cos(angle);
		final double sinAngle = Math.sin(angle);

		//
		// Step 1 : Compute (x1, y1)
		//
		final double x1 = (cosAngle * dx2 + sinAngle * dy2);
		final double y1 = (-sinAngle * dx2 + cosAngle * dy2);
		// Ensure radii are large enough
		rx = Math.abs(rx);
		ry = Math.abs(ry);
		double prx = rx * rx;
		double pry = ry * ry;
		final double px1 = x1 * x1;
		final double py1 = y1 * y1;
		// check that radii are large enough
		final double radiiCheck = px1/prx + py1/pry;
		if (radiiCheck > 1) {
			rx = Math.sqrt(radiiCheck) * rx;
			ry = Math.sqrt(radiiCheck) * ry;
			prx = rx * rx;
			pry = ry * ry;
		}

		//
		// Step 2 : Compute (cx1, cy1)
		//
		double sign = (largeArcFlag == sweepFlag) ? -1 : 1;
		double sq = ((prx*pry)-(prx*py1)-(pry*px1)) / ((prx*py1)+(pry*px1));
		sq = (sq < 0) ? 0 : sq;
		final double coef = (sign * Math.sqrt(sq));
		final double cx1 = coef * ((rx * y1) / ry);
		final double cy1 = coef * -((ry * x1) / rx);

		//
		// Step 3 : Compute (cx, cy) from (cx1, cy1)
		//
		final double sx2 = (x0 + x) / 2.0;
		final double sy2 = (y0 + y) / 2.0;
		final double cx = sx2 + (cosAngle * cx1 - sinAngle * cy1);
		final double cy = sy2 + (sinAngle * cx1 + cosAngle * cy1);

		//
		// Step 4 : Compute the angleStart (angle1) and the angleExtent (dangle)
		//
		final double ux = (x1 - cx1) / rx;
		final double uy = (y1 - cy1) / ry;
		final double vx = (-x1 - cx1) / rx;
		final double vy = (-y1 - cy1) / ry;
		double p, n;
		// Compute the angle start
		n = Math.sqrt((ux * ux) + (uy * uy));
		p = ux; // (1 * ux) + (0 * uy)
		sign = (uy < 0) ? -1d : 1d;
		double angleStart = Math.toDegrees(sign * Math.acos(p / n));

		// Compute the angle extent
		n = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
		p = ux * vx + uy * vy;
		sign = (ux * vy - uy * vx < 0) ? -1d : 1d;
		double angleExtent = Math.toDegrees(sign * Math.acos(p / n));
		if(!sweepFlag && angleExtent > 0) {
			angleExtent -= 360f;
		} else if (sweepFlag && angleExtent < 0) {
			angleExtent += 360f;
		}
		angleExtent %= 360f;
		angleStart %= 360f;

		//
		// We can now build the resulting Arc2D in double precision
		//
		Arc2D.Double arc = new Arc2D.Double(Arc2D.OPEN);
		arc.x = cx - rx;
		arc.y = cy - ry;
		arc.width = rx * 2.0;
		arc.height = ry * 2.0;
		arc.start = -angleStart;
		arc.extent = -angleExtent;

		return arc;
	}

}
