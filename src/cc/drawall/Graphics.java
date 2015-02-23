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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Transform;

import com.sun.javafx.geom.Area;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.RoundRectangle2D;
import com.sun.javafx.tk.Toolkit;

/** Graphics context for all drawing operations done by Importer plugins.
  * Provides methods for setting graphical state, constructing paths and drawing them.
  * Graphical state includes the current transform matrix, clipping path,
  * font, stroke type, stroking color and filling color.
  * Path construction can be done using straight lines, Bézier curves, `closePath` instructions
  * and text.
  * Path drawing is either stroking, filling or both. */
public class Graphics {
	Drawing drawing = new Drawing();

	/* Graphical state information */
	private final GraphicsContext g = new Canvas().getGraphicsContext2D();
	private final Deque<Area> clips = new ArrayDeque<>(); {
		clips.push(new Area(new RoundRectangle2D(
			-Float.MAX_VALUE / 2, -Float.MAX_VALUE / 2,
			Float.MAX_VALUE, Float.MAX_VALUE, 0, 0)));
	}

	///////////////////////
	// Path construction //
	///////////////////////

	/** Set the current point without drawing anything.
	  * @param relative whether to interpret coordinates as relative to the current point
	  * @param x the target abscissa
	  * @param y the target ordinate */
	public void moveTo(final float x, final float y) {
		g.moveTo(x, y);
	}

	/** Appends a Bézier curve specified by a list of coordinates to the current path.
	  * The starting point is always the current point; further control points should
	  * be specified in order as pairs of x, y coordinates. Thus, the order of the curve
	  * is equal to the number of given coordinate pairs.
	  * For example, if only one pair is given, the result is a first-order Bézier curve,
	  * aka a straight line.
	  * @param relative whether to interpret coordinates as relative to the current point
	  * @param points the array containing the control point coordinates of the desired curve */
	public void lineTo(final float... points) {
		if (points.length == 2) {
			g.lineTo(points[0], points[1]);
		} else if (points.length == 4) {
			g.quadraticCurveTo(points[0], points[1], points[2], points[3]);
		} else if (points.length == 6) {
			g.bezierCurveTo(points[0], points[1], points[2], points[3], points[4], points[5]);
		} else {
			throw new IllegalArgumentException("Invalid length for point array: " + points.length);
		}
	}

	/** Closes the current path by drawing a straight line back to the coordinates of the last
	  * moveTo. If the path is already closed then this method has no effect. */
	public void closePath() {
		g.closePath();
	}

	/** Resets the path to its initial, empty state.
	  * @return this Graphics */
	public Graphics resetPath() {
		g.beginPath();
		return this;
	}

	/** Appends the geometry of the specified Shape to the path. The winding rule of the
	  * specified Shape is ignored.
	  * @param shape whose geometry is appended to the path */
	public void append(final Shape shape) {
		getPath().append(getArea(shape).getPathIterator(null), false);
	}

	private static Area getArea(Shape shape) {
		try {
			final Method method = Shape.class.getDeclaredMethod("getTransformedArea");
			method.setAccessible(true);
			return (Area) method.invoke(shape);
		} catch (final IllegalAccessException | NoSuchMethodException
				| InvocationTargetException e) {
			throw new AssertionError("Reflection error", e);
		}
	}

	/** Intersects the clipping area with the interior of the specified shape.
	  * @param s the Shape to be intersected with the current Clip  */
	public void clip(final Shape s) {
		clips.peek().intersect(getArea(s));
	}

	/** Appends an outline of the specified text to the path. Text is rendered using the
	  * current font and starting at the current point.
	  * @param text the text to be outlined */
	public void charpath(final String text) {
		append(new Text(text));
	}

	////////////////////////
	// Drawing operations //
	////////////////////////

	public Path2D getPath() {
		try {
			final Field field = GraphicsContext.class.getDeclaredField("path");
			field.setAccessible(true);
			return (Path2D) field.get(g);
		} catch (final IllegalAccessException | NoSuchFieldException e) {
			throw new AssertionError("Reflection error", e);
		}
	}

	/** Stroke the current path with the stroking Color.
	  * @return this Graphics */
	public Graphics stroke() {
		if (g.getStroke() == null) {
			return this;
		}
		Area area = new Area(Toolkit.getToolkit().createStrokedShape(getPath(),
			StrokeType.CENTERED, g.getLineWidth(), g.getLineCap(), g.getLineJoin(),
			(float) g.getMiterLimit(), null, 0));
		area.intersect(clips.peek());
		drawing.paint((Color) g.getStroke(), area);
		return this;
	}

	/** Fill the current path with the filling Color.
	  * @return this Graphics */
	public Graphics fill() {
		if (g.getFill() == null) {
			return this;
		}
		g.closePath();
		Area area = new Area(getPath());
		area.intersect(clips.peek());
		drawing.paint((Color) g.getFill(), area);
		return this;
	}

	///////////////////////
	// Graphics settings //
	///////////////////////

	/** Set the stroke width.
	  * @param width the width of stroked paths
	  * @see java.awt.BasicStroke */
	public void setStrokeWidth(final float width) {
		g.setLineWidth(width);
	}

	/** Set the dashing style.
	  * @param dash an array representing the dashing pattern
	  * @param phase an offset from the start of the dashing pattern
	  * @see java.awt.BasicStroke */
	public void setStrokeDash(final float[] dash, final float phase) {
		// g.setStrokeDashOffset(phase);
	}

	/** Changes the line cap style.
	  * @param cap either BasicStroke.CAP_BUTT, BasicStroke.CAP_ROUND or BasicStroke.CAP_SQUARE
	  * @see java.awt.BasicStroke */
	public void setLineCap(final StrokeLineCap cap) {
		// path.setStrokeLineCap(cap);
	}

	/** Changes the line join style.
	  * @param join either BasicStroke.JOIN_BEVEL, BasicStroke.JOIN_MITER BasicStroke.JOIN_ROUND
	  * @see java.awt.BasicStroke */
	public void setLineJoin(final StrokeLineJoin join) {
		g.setLineJoin(join);
	}

	/** Set the limit to trim a line join when the join style is JOIN_MITER.
	  * A line join is trimmed when the ratio of miter length to stroke width is greater than
	  * the specified value.
	  * @param limit the maximum allowed ratio of miter length to stroke width
	  * @see java.awt.BasicStroke */
	public void setMiterLimit(final float limit) {
		g.setMiterLimit(limit);
	}

	public void setStrokeColor(final Color strokeColor) {
		g.setStroke(strokeColor);
	}

	public void setFillColor(final Color fillColor) {
		g.setFill(fillColor);
	}

	/** Sets the winding rule for filling operations.
	  * @see java.awt.geom.GeneralPath
	  * @param rule the winding rule to use
	  * @return this Graphics */
	public Graphics setWindingRule(FillRule rule) {
		g.setFillRule(rule);
		return this;
	}

	public void transform(Transform transform) {
		g.transform(new Affine(transform));
	}

	/** Set the current font to the one described by the specified string. If no fitting font is
	  * found in the system, an attempt is made to load one from the ressource files. */
	public void setFont(final String fontDescriptor) {
		g.setFont(new Font(fontDescriptor, g.getFont().getSize()));
	}

	public Font getFont() {
		return g.getFont();
	}

	/** Set the font size.
	  * @param fontSize the desired font size, in points */
	public void setFontSize(final float fontSize) {
		g.setFont(new Font(g.getFont().getFamily(), fontSize));
	}

	/** Pushes a snapshot of the graphical state on the save stack.
	  * @see restore() */
	public void save() {
		g.save();
		clips.push(new Area(clips.peek()));
	}

	/** Reverts the graphical state to the latest snapshot.
	  * The used snapshot is popped from the save stack, so that a later restore()
	  * will restore an earlier snapshot. */
	public void restore() {
		g.restore();
		clips.pop();
	}
}
