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
 * © 2014-2015 Victor Adam
 */

package cc.drawall;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
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

	Drawing drawing = new Drawing();

	/* Graphical state information */
	private final AffineTransform ctm = new AffineTransform();
	private Area clippath = new Area(new Rectangle2D.Float(
				-Float.MAX_VALUE/2, -Float.MAX_VALUE/2,
				Float.MAX_VALUE, Float.MAX_VALUE));
	private final Path2D path = new Path2D.Float();
	private Color strokeColor = Color.BLACK;
	private Color fillColor;
	private Font font;
	private float fontSize = 1;
	private BasicStroke stroke = new BasicStroke(1, 0, 0, 10);

	/* Saved graphical context. */
	private Graphics prev;

	/* First control point of a following smooth curve */
	private Point2D.Float smooth;

	///////////////////////
	// Path construction //
	///////////////////////

	/** Set the current point without drawing anything.
	  * @param relative whether to interpret coordinates as relative to the current point
	  * @param x the target abscissa
	  * @param y the target ordinate */
	public void moveTo(final boolean relative, final float x, final float y) {
		final float[] points = {x, y};
		transform(relative, points);
		path.moveTo(points[0], points[1]);
	}

	/** Appends a Bézier curve specified by a list of coordinates to the current path.
	  * The starting point is always the current point; further control points should
	  * be specified in order as pairs of x, y coordinates. Thus, the order of the curve
	  * is equal to the number of given coordinate pairs.
	  * For example, if only one pair is given, the result is a first-order Bézier curve,
	  * aka a straight line.
	  * @param relative whether to interpret coordinates as relative to the current point
	  * @param points the array containing the control point coordinates of the desired curve */
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

	/** Appends an elliptical arc specified by endpoint parameterization to the current path.
	  * @param relative whether to interpret coordinates as relative to the current point
	  * @param xAxisRotation indicates how the ellipse as a whole is rotated
	  * @param largeArcFlag whether to choose one of the larger arc sweeps
	  * @param sweepFlag whether to choose one of the counterclockwise arc sweeps
	  * @param points the coordinates of the center and end point of the ellipse to be drawn */
	public void arcTo(final boolean relative, final float xAxisRotation,
			final boolean largeArcFlag, final boolean sweepFlag, final float... points) {
		final Point2D radius = new Point2D.Float(points[0], points[1]);
		final float[] end = {points[2], points[3]};
		transform(relative, end);
		ctm.deltaTransform(radius, radius);
		final Point2D p0 = path.getCurrentPoint();
		final double x0 = p0.getX();
		final double y0 = p0.getY();
		double rx = Math.abs(radius.getX());
		double ry = Math.abs(radius.getY());
		final double angle = Math.toRadians(xAxisRotation % 360.0);

		// Based on w3c’s SVG specification, Appendix F.6.5
		// Step 1 : Compute (x1, y1)
		final double dx2 = (x0 - end[0]) / 2.0;
		final double dy2 = (y0 - end[1]) / 2.0;
		final double cos = Math.cos(angle);
		final double sin = Math.sin(angle);
		final double x1 =  cos * dx2 + sin * dy2;
		final double y1 = -sin * dx2 + cos * dy2;

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
		final double cx = (x0 + end[0]) / 2.0 + cos * cx1 - sin * cy1;
		final double cy = (y0 + end[1]) / 2.0 + sin * cx1 + cos * cy1;

		// Step 4 : Compute the angleStart and the angleExtent
		final double ux = (x1 - cx1) / rx;
		final double uy = (y1 - cy1) / ry;
		final double angleStart = Math.toDegrees((uy < 0 ? -1d : 1d) * Math.acos(
				ux / Math.sqrt(ux * ux + uy * uy)));
		final double vx = (-x1 - cx1) / rx;
		final double vy = (-y1 - cy1) / ry;
		final double p = ux * vx + uy * vy;
		final double angleExtent = (Math.toDegrees((ux * vy < uy * vx ? -1.0 : 1.0)
				* Math.acos(p / Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy))))
				+ (sweepFlag ? 360.0 : -360.0)) % 360.0;

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
		log.fine("g.reset()");
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
	public void clip(Shape s) {
		clippath.intersect(new Area(s));
	}

	/** Returns the smallest rectangle that completely encloses the current path.
	  * @return the smallest rectangle that completely encloses the current path */
	public Rectangle2D pathBounds() {
		return path.getBounds2D();
	}

	/** Appends an outline of the specified text to the path.
	  * Text is rendered using the current font and starting at the current point.
	  * @param text the text to be outlined */
	public void charpath(final String text) {
		assert font != null : "Undefined font";
		final AffineTransform t = relativeTransform();
		t.scale(1, -1);
		path.append(t.createTransformedShape(font.createGlyphVector(
			new FontRenderContext(null, true, false), text).getOutline()), false);
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

	/** Stroke the current path with the stroking Color. */
	public Graphics stroke() {
		if (strokeColor == null || path.getCurrentPoint() == null) {
			return this;
		}
		try {
			final Shape inverse = ctm.createInverse().createTransformedShape(path);
			final Shape stroked = stroke.createStrokedShape(inverse);
			drawing.paint(strokeColor, clipped(ctm.createTransformedShape(stroked)));
		} catch (final NoninvertibleTransformException e) {
			// Non-invertible transforms squash any shape to empty areas
			log.finer(e.toString());
		}
		return this;
	}

	/** Fill the current path with the filling Color.
	  * @param windingRule the winding rule to use
	  * @see java.awt.geom.GeneralPath */
	public Graphics fill(final int windingRule) {
		if (fillColor == null || path.getCurrentPoint() == null) {
			return this;
		}
		path.closePath();
		path.setWindingRule(windingRule);
		Area area = new Area(path);
		area.intersect(clippath);
		drawing.paint(fillColor, area);
		return this;
	}

	public void draw() {
		stroke();
		fill(Path2D.WIND_NON_ZERO);
		Point2D p = path.getCurrentPoint();
		path.reset();
		path.moveTo(p == null ? 0 : p.getX(), p == null ? 0 : p.getY());
	}

	private Area clipped(Shape shape) {
		Area result = new Area(shape);
		result.intersect(clippath);
		return result;
	}

	///////////////////////
	// Graphics settings //
	///////////////////////

	/** Set the stroke width.
	  * @param width the width of stroked paths
	  * @see java.awt.BasicStroke */
	public void setStrokeWidth(final float width) {
		setStroke("width", width);
	}

	/** Set the dashing style.
	  * @param dash an array representing the dashing pattern
	  * @param phase an offset from the start of the dashing pattern
	  * @see java.awt.BasicStroke */
	public void setStrokeDash(final float[] dash, final float phase) {
		setStroke("dash", dash);
		setStroke("phase", phase);
	}

	/** Changes the line cap style.
	  * @param cap either BasicStroke.CAP_BUTT, BasicStroke.CAP_ROUND or BasicStroke.CAP_SQUARE
	  * @see java.awt.BasicStroke */
	public void setLineCap(final int cap) {
		setStroke("cap", cap);
	}

	/** Changes the line join style.
	  * @param join either BasicStroke.JOIN_BEVEL, BasicStroke.JOIN_MITER BasicStroke.JOIN_ROUND
	  * @see java.awt.BasicStroke */
	public void setLineJoin(final int join) {
		setStroke("join", join);
	}

	/** Set the limit to trim a line join when the join style is JOIN_MITER.
	  * A line join is trimmed when the ratio of miter length to stroke width is greater than
	  * the specified value.
	  * @param limit the maximum allowed ratio of miter length to stroke width
	  * @see java.awt.BasicStroke */
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

	public void setStrokeColor(final Color strokeColor) {
		this.strokeColor = strokeColor;
	}

	public void setFillColor(final Color fillColor) {
		this.fillColor = fillColor;
	}

	public Path2D getPath() {
		return (Path2D) path.clone();
	}

	public AffineTransform getTransform() {
		return ctm;
	}

	public Area getClip() {
		return clippath;
	}

	/** Set the current font to the one described by the specified string.
	  * If no fitting font is found in the system, an attempt is made to load one
	  * from the ressource files. */
	public void setFont(final String fontDescriptor) {
		log.fine("Setting font to: " + fontDescriptor);
		font = new Font(fontDescriptor, 0, (int) fontSize)
			.deriveFont(AffineTransform.getScaleInstance(1, -1));
	}

	public Font getFont() {
		return font;
	}

	/** Set the font size.
	  * @param fontSize the desired font size, in points */
	public void setFontSize(final float fontSize) {
		this.fontSize = fontSize;
		font = font == null ? null : font.deriveFont(fontSize);
	}

	/** Pushes a snapshot of the graphical state on the save stack.
	  * @see restore() */
	public void save() {
		final Graphics copy = new Graphics();
		copy.copy(this);
		prev = copy;
	}

	/** Reverts the graphical state to the latest snapshot.
	  * The used snapshot is popped from the save stack, so that a later restore()
	  * will restore an earlier snapshot. */
	public Graphics restore() {
		copy(prev);
		return this;
	}

	private void copy(final Graphics that) {
		this.ctm.setTransform(that.ctm);
		this.clippath = (Area) that.clippath.clone();
		this.strokeColor = that.strokeColor;
		this.fillColor = that.fillColor;
		this.font = that.font;
		this.stroke = new BasicStroke(stroke.getLineWidth(), stroke.getEndCap(),
				stroke.getLineJoin(), stroke.getMiterLimit(), stroke.getDashArray(),
				stroke.getDashPhase());
		this.drawing = that.drawing;
		this.prev = that.prev;
	}
}
