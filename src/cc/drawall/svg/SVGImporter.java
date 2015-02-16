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

package cc.drawall.svg;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import cc.drawall.Graphics;
import cc.drawall.Importer;

/** An Importer for SVG images. */
public class SVGImporter extends ZalgoParser implements Importer {
	private static final Pattern SVG_COMMAND = Pattern.compile("[mzlhvcsqtaMZLHVCSQTA]");
	private static final Pattern ATTR = Pattern.compile("[^'\";]*");

	private static final Map<String, Float> fontSizes = new HashMap<>(); {
		fontSizes.put("xx-small", 125 / 18f);
		fontSizes.put("x-small", 25 / 3f);
		fontSizes.put("small", 10f);
		fontSizes.put("medium", 12f);
		fontSizes.put("large", 14.4f);
		fontSizes.put("x-large", 17.28f);
		fontSizes.put("xx-large", 20.736f);
	}

	private static final Map<String, Float> unitMap = new HashMap<>(); {
		// Conversion ratios to pixels, as given by the SVG spec in 7.10: Units
		unitMap.put("px", 1f);
		unitMap.put("pt", 1.25f);
		unitMap.put("pc", 15f);
		unitMap.put("mm", 3.543307f);
		unitMap.put("cm", 35.43307f);
		unitMap.put("in", 90f);
	}

	private static final List<String> caps = Arrays.asList("butt", "round", "square");
	private static final List<String> joins = Arrays.asList("miter", "round", "bevel");
	private static final List<String> defs = Arrays.asList("defs", "symbol", "clipPath", "linearGradient");

	private final Graphics g = new Graphics();
	private final Deque<String> idStack = new ArrayDeque<>();
	private final Map<String, Color> gradients = new HashMap<>();
	private final Map<String, Path2D> paths = new HashMap<>();
	private int inText;

	float width = Float.MAX_VALUE, height = Float.MAX_VALUE;
	String href;
	String id;
	float x, y, x2, y2, rx, ry, cx, cy;

	private final Map<String, Runnable> attrHandlers = new HashMap<>(); {
		attrHandlers.put("d", () -> parsePathData());
		attrHandlers.put("points", () -> {
			parsePathData();
			if (currentTag.equals("polygon")) {
				g.closePath();
			}
		});
		attrHandlers.put("r", () -> rx = ry = getFloat());
		attrHandlers.put("x", () -> x = getFloat());
		attrHandlers.put("y", () -> y = getFloat());
		attrHandlers.put("x1", () -> x = getFloat());
		attrHandlers.put("y1", () -> y = getFloat());
		attrHandlers.put("x2", () -> x2 = getFloat());
		attrHandlers.put("y2", () -> y2 = getFloat());
		attrHandlers.put("rx", () -> rx = getFloat());
		attrHandlers.put("ry", () -> ry = getFloat());
		attrHandlers.put("cx", () -> cx = getFloat());
		attrHandlers.put("cy", () -> cy = getFloat());
		attrHandlers.put("width", () -> width = getFloat());
		attrHandlers.put("height", () -> height = getFloat());

		attrHandlers.put("id", () -> id = '#' + val());
		attrHandlers.put("xlink:href", () -> href = val());
		attrHandlers.put("display", () -> {
			if (val().equals("none")) {
				g.clip(new Rectangle2D.Float(0, 0, 0, 0));
			}
		});
		attrHandlers.put("fill", () -> parseColor(g::setFillColor));
		attrHandlers.put("clip-path", () -> {
			final String url = val();
			if (paths.containsKey(url)) {
				g.clip(g.getTransform().createTransformedShape(paths.get(url)));
			}
		});
		attrHandlers.put("stroke", () -> parseColor(g::setStrokeColor));
		attrHandlers.put("stop-color", () -> parseColor(color ->
					gradients.put(idStack.peek(), color)));
		attrHandlers.put("transform", () -> g.getTransform().concatenate(parseTransform()));
		attrHandlers.put("style", () -> {
			scanner.useDelimiter("\\s*:\\s*");
			while (scanner.hasNext(ATTR)) {
				String attr = scanner.next();
				scanner.skip("\\s*:\\s*");
				attribute(attr);
				scanner.skip(";?");
			}
		});
		attrHandlers.put("stroke-width", () -> g.setStrokeWidth(getFloat()));
		attrHandlers.put("stroke-linecap", () -> g.setLineCap(caps.indexOf(val())));
		attrHandlers.put("stroke-linejoin", () -> g.setLineJoin(joins.indexOf(val())));
		attrHandlers.put("stroke-miterlimit", () -> g.setMiterLimit(getFloat()));
		attrHandlers.put("font-size", () -> g.setFontSize(getFloat()));
		attrHandlers.put("font-family", () -> g.setFont(val()));
		// font-weight, text-align, text-anchor
	}

	private final String val() {
		return scanner.skip(ATTR).match().group(0);
	}

	@Override
	protected void attribute(String key) {
		attrHandlers.getOrDefault(key.trim(), () -> val()).run();
	}

	@Override
	@SuppressWarnings("resource")
	public Graphics process(final ReadableByteChannel input) {
		g.setFillColor(Color.BLACK);
		g.setStrokeColor(null);
		parse(new Scanner(input, "utf-8"));
		return g;
	}

	private float getFloat() {
		String floatString = val();
		if (fontSizes.containsKey(floatString)) {
			return fontSizes.get(floatString);
		}
		final int index = floatString.length() - 2; // all SVG units are 2 chars long
		final Float multiplier = index < 0 ? null : unitMap.get(floatString.substring(index));
		return multiplier == null ? Float.parseFloat(floatString)
			: multiplier * Float.parseFloat(floatString.substring(0, index));
	}

	@Override
	public void tag(final String name) {
		g.save();
	}

	@Override
	public void gt(final String name) {
		if (defs.contains(name) || !idStack.isEmpty()) {
			// TODO: something about these urls
			idStack.push("url(" + id + ")");
			if (name.equals("linearGradient") && href != null) {
				gradients.put(idStack.peek(), gradients.get("url(" + href + ")"));
			}
		}

		switch (name) {
		case "svg":
			g.clip(new Rectangle2D.Float(0, 0, width, height));
			break;
		case "use":
			g.append(paths.getOrDefault("url(" + href + ")", new Path2D.Float()));
			break;
		case "radialGradient":
			gradients.put("url(" + id + ")", null);
			break;
		case "line":
			g.moveTo(false, x, y);
			g.lineTo(false, x2, y2);
			break;
		case "circle":
		case "ellipse":
			g.append(g.getTransform().createTransformedShape(new Ellipse2D.Float(
					cx - rx, cy - ry, 2 * rx, 2 * ry)));
			break;
		case "rect":
			if (rx > 0f || ry > 0f) {
				throw new InputMismatchException("Rounded rectangles are not handled.");
			}
			g.append(g.getTransform().createTransformedShape(new Rectangle2D.Float(
					x, y, width, height)));
			break;
		case "text":
			System.out.println("Texting: " + x + "; " + y);
			g.moveTo(false, x, y);
			++inText;
			break;
		case "tspan":
			System.out.println("Texting: " + x2 + "; " + y2);
			g.moveTo(false, x2, y2);
			++inText;
			break;
		default:
		}

		x2 = y2 = Float.NaN;
		width = height = x = y = rx = ry = cx = cy = 0;
	}

	@Override
	public void endTag(String name) {
		if (idStack.isEmpty()) {
			g.draw();
			g.reset();
		} else {
			paths.put(idStack.pop(), g.getPath());
			if (defs.contains(name)) {
				g.reset();
			}
		}
		if (inText > 0) {
			--inText;
		}
		g.restore();
	}

	private void parsePathData() {
		scanner.useDelimiter("(?<=[mzlhvcsqtaMZLHVCSQTA])\\s*|"
				+ "[\\s,]*(?:[\\s,]|(?=[^\\deE.-])|(?<![eE])(?=-))");
		char cmd = 'M';

		while (scanner.hasNext(ATTR)) {
			if (scanner.hasNext(SVG_COMMAND)) {
				cmd = scanner.next(SVG_COMMAND).charAt(0);
			}
			final boolean relative = Character.isLowerCase(cmd);
			if (g.getCurrentPoint() == null && relative) {
				g.moveTo(false, 0, 0);
			}
			switch (Character.toUpperCase(cmd)) {
			case 'M':
				g.moveTo(relative, scanner.nextFloat(), scanner.nextFloat());
				--cmd;
				break;
			case 'L':
				g.lineTo(relative, scanner.nextFloat(), scanner.nextFloat());
				break;
			case 'Q':
				g.lineTo(relative, scanner.nextFloat(), scanner.nextFloat(),
						scanner.nextFloat(), scanner.nextFloat());
				break;
			case 'C':
				g.lineTo(relative, scanner.nextFloat(), scanner.nextFloat(),
						scanner.nextFloat(), scanner.nextFloat(),
						scanner.nextFloat(), scanner.nextFloat());
				break;
			case 'Z':
				g.closePath();
				if (idStack.isEmpty() && g.getFillColor() == null) {
					g.draw();
				}
				cmd = '!';
				break;
			case 'A':
				final float rx = scanner.nextFloat(), ry = scanner.nextFloat();
				g.arcTo(relative, scanner.nextFloat(),
						scanner.nextInt() != 0, scanner.nextInt() != 0,
						rx, ry, scanner.nextFloat(), scanner.nextFloat());
				break;
			case 'H': // horizontal
				g.lineTo(relative, scanner.nextFloat(), Float.NaN);
				break;
			case 'V': // vertical
				g.lineTo(relative, Float.NaN, scanner.nextFloat());
				break;
			case 'T': // smooth quadratic
				g.lineTo(relative, Float.NaN, Float.NaN, scanner.nextFloat(), scanner.nextFloat());
				break;
			case 'S': // smooth cubic
				g.lineTo(relative, Float.NaN, Float.NaN, scanner.nextFloat(),
						scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
				break;
			default:
				assert false : "Unknown path operator: " + cmd;
			}
		}
	}

	private void parseColor(final Consumer<Color> callback) {
		final String colorName = val();
		// System.out.println(colorName);
		if (gradients.containsKey(colorName)) {
			callback.accept(gradients.get(colorName));
		}
		if (colorName.startsWith("url")) {
			return;
		}
		if (colorName.equals("none")) {
			callback.accept(null);
			return;
		}
		final javafx.scene.paint.Color color = javafx.scene.paint.Color.web(colorName);
		callback.accept(new Color((float) color.getRed(),
					(float) color.getGreen(), (float) color.getBlue()));
	}

	private AffineTransform parseTransform() {
		scanner.useDelimiter("[(, )]+");
		final AffineTransform result = new AffineTransform();
		while (scanner.hasNext(ATTR)) {
			String tmp = scanner.next();
			switch (tmp) {
			case "matrix":
				result.concatenate(new AffineTransform(
						scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat(),
						scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat()));
				break;
			case "translate":
				result.translate(scanner.nextFloat(), scanner.nextFloat());
				break;
			case "rotate":
				result.rotate(Math.toRadians(scanner.nextFloat()));
				break;
			case "scale":
				final float xScale = scanner.nextFloat();
				final float yScale = scanner.hasNextFloat() ? scanner.nextFloat() : xScale;
				result.scale(xScale, yScale);
				break;
			case "skewX":
			case "skewY":
			default:
				throw new InputMismatchException("Unhandled transform");
			}
		}
		scanner.skip("\\)");
		if (g.getCurrentPoint() != null) {
			assert false;
			// Shape transformed = result.createTransformedShape(g.getPath());
			// g.reset();
			// g.append(transformed);
		}
		return result;
	}

	@Override
	public void text(final String str) {
		String text = str.trim();
		if (inText > 0 && !text.isEmpty()) {
			System.out.println("Printing <" + text + "> at " + g.getCurrentPoint());
			g.charpath(text);
		}
	}
}
