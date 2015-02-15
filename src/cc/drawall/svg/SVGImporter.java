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
import java.io.IOException;
import java.io.StringReader;
import java.nio.channels.Channels;
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
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import cc.drawall.Graphics;
import cc.drawall.Importer;

/** An Importer for SVG images. */
public class SVGImporter extends DefaultHandler implements Importer {
	private static final Logger log = Logger.getLogger(SVGImporter.class.getName());

	private static final Pattern SVG_COMMAND = Pattern.compile("[mzlhvcsqtaMZLHVCSQTA]");

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
	private Attributes attributes;
	private boolean inText;

	private final Map<String, Consumer<String>> attrHandlers = new HashMap<>(); {
		attrHandlers.put("display", v -> {
			if (v.equals("none")) {
				g.clip(new Rectangle2D.Float(0, 0, 0, 0));
			}
		});
		attrHandlers.put("fill", v -> parseColor(v, g::setFillColor));
		attrHandlers.put("clip-path", v -> {
			if (paths.containsKey(v)) {
				g.clip(g.getTransform().createTransformedShape(paths.get(v)));
			}
		});
		attrHandlers.put("stroke", v -> parseColor(v, g::setStrokeColor));
		attrHandlers.put("stop-color", v -> parseColor(v, color ->
					gradients.put(idStack.peek(), color)));
		attrHandlers.put("transform", v -> g.getTransform().concatenate(parseTransform(v)));
		attrHandlers.put("style", v -> Arrays.stream(v.split(";")).forEach(prop ->
				handleAttr(prop.split(":")[0], prop.split(":")[1])));
		attrHandlers.put("stroke-width", v -> g.setStrokeWidth(parseLength(v)));
		attrHandlers.put("stroke-linecap", v -> g.setLineCap(caps.indexOf(v)));
		attrHandlers.put("stroke-linejoin", v -> g.setLineJoin(joins.indexOf(v)));
		attrHandlers.put("stroke-miterlimit", v -> g.setMiterLimit(parseLength(v)));
		attrHandlers.put("font-size", v -> g.setFontSize(parseLength(v)));
		attrHandlers.put("font-family", v -> g.setFont(v));
		// font-weight, text-align, text-anchor
	}

	private void handleAttr(final String name, final String value) {
		log.fine(name + "=" + value);
		attrHandlers.getOrDefault(name.trim(), v -> {/*NOOP*/}).accept(value.trim());
	}

	@Override
	public Graphics process(final ReadableByteChannel input) {
		g.setFillColor(Color.BLACK);
		g.setStrokeColor(null);
		try {
			SAXParserFactory.newInstance().newSAXParser().parse(
					Channels.newInputStream(input), this);
		} catch (final ParserConfigurationException | IOException e) {
			assert false : "XML error : " + e;
		} catch (final SAXException e) {
			final RuntimeException wrapper = new InputMismatchException(
					"Invalid XML file" + e.getMessage());
			wrapper.initCause(e);
			throw wrapper;
		}
		return g;
	}

	@Override
	public InputSource resolveEntity(final String publicId, final String systemId) {
		return new InputSource(new StringReader(""));
	}

	/** Parses a floating-point number, respecting SVG units. */
	private static float parseLength(final String floatString) {
		if (fontSizes.containsKey(floatString)) {
			return fontSizes.get(floatString);
		}
		final int index = floatString.length() - 2;  // all SVG units are 2 chars long
		final Float multiplier = index < 0 ? null : unitMap.get(floatString.substring(index));
		return multiplier == null ? Float.parseFloat(floatString)
			: multiplier * Float.parseFloat(floatString.substring(0, index));
	}

	private float getFloat(final String name, final float def) {
		final String value = attributes.getValue(name);
		return value == null ? def : parseLength(value.trim().split(" ")[0]);
	}

	@Override
	public void startElement(final String namespace, final String local,
			final String name, final Attributes attributes) {
		g.save();
		this.attributes = attributes;
		for (int i = 0; i < attributes.getLength(); i++) {
			handleAttr(attributes.getLocalName(i), attributes.getValue(i));
		}

		if (defs.contains(name) || !idStack.isEmpty()) {
			idStack.push("url(#" + attributes.getValue("id") + ")");
			log.info("<" + name + ">" + idStack);
			if (name.equals("linearGradient") && attributes.getValue("xlink:href") != null) {
				gradients.put(idStack.peek(), gradients.get("url(" + attributes.getValue("xlink:href") + ")"));
			}
		}

		switch (name) {
		case "svg":
			g.clip(new Rectangle2D.Float(0, 0, getFloat("width", Float.MAX_VALUE),
						getFloat("height", Float.MAX_VALUE)));
			break;
		case "use":
			g.append(paths.get("url(" + attributes.getValue("xlink:href") + ")"));
			break;
		case "radialGradient":
			gradients.put("url(#" + attributes.getValue("id") + ")", null);
			break;
		case "line":
			parsePathData("M " + getFloat("x1", 0f) + "," + getFloat("y1", 0f)
			   + "," + getFloat("x2", 0f) + "," + getFloat("y2", 0f));
			break;
		case "circle":
		case "ellipse":
			final float rx = getFloat("rx", getFloat("r", 0f));
			final float ry = getFloat("ry", getFloat("r", 0f));
			final float cx = getFloat("cx", 0f);
			final float cy = getFloat("cy", 0f);
			g.append(g.getTransform().createTransformedShape(new Ellipse2D.Float(
					cx - rx, cy - ry, 2 * rx, 2 * ry)));
			break;
		case "polygon":
			parsePathData("M" + attributes.getValue("points") + "z");
			break;
		case "polyline":
			parsePathData("M" + attributes.getValue("points"));
			break;
		case "rect":
			if (getFloat("rx", 0f) > 0f || getFloat("ry", 0f) > 0f) {
				log.severe("Rounded rectangles are not handled.");
			}
			g.append(g.getTransform().createTransformedShape(new Rectangle2D.Float(
					getFloat("x", 0f), getFloat("y", 0f),
					getFloat("width", 0f), getFloat("height", 0f))));
			break;
		case "text":
			g.moveTo(false, getFloat("x", 0), getFloat("y", 0));
			inText = true;
			break;
		case "tspan":
			g.moveTo(false, getFloat("x", Float.NaN), getFloat("y", Float.NaN));
			break;
		case "path":
			parsePathData(attributes.getValue("d"));
			break;
		default:
			log.info("Unhandled tag: " + name);
		}
	}

	@Override
	public void endElement(final String namespace, final String local, final String name) {
		log.fine("</" + name + ">");
		if (idStack.isEmpty()) {
			g.draw();
			g.reset();
		} else {
			log.info("Adding path: " + idStack.peek());
			paths.put(idStack.pop(), g.getPath());
			log.info("Current point: " + g.getPath().getCurrentPoint());
			if (defs.contains(name)) {
				g.reset();
			}
		}
		inText &= !name.equals("text");
		g.restore();
	}

	private void parsePathData(final String data) {
		@SuppressWarnings("resource")
		final Scanner scanner = new Scanner(data);
		scanner.useDelimiter("(?<=[mzlhvcsqtaMZLHVCSQTA])\\s*|"
				+ "[\\s,]*(?:[\\s,]|(?=[^\\deE.-])|(?<![eE])(?=-))");
		char cmd = '!';

		while (scanner.hasNext()) {
			if (scanner.hasNext(SVG_COMMAND)) {
				cmd = scanner.next(SVG_COMMAND).charAt(0);
			}
			final boolean relative = Character.isLowerCase(cmd);
			if (g.getCurrentPoint() == null && relative) {
				g.moveTo(false, 0, 0);
			}
			log.finest("cmd: " + cmd);
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

	private void parseColor(final String colorName, final Consumer<Color> callback) {
		log.info("Parsing color: <" + colorName + ">");
		if (gradients.containsKey(colorName)) {
			log.warning("Gradient " + colorName + " maps to " + gradients.get(colorName));
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

	private static AffineTransform parseTransform(final String transform) {
		@SuppressWarnings("resource")
		final Scanner scanner = new Scanner(transform);
		scanner.useDelimiter("[(, )]+");
		final AffineTransform result = new AffineTransform();
		while (scanner.hasNext()) {
			switch (scanner.next()) {
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
				log.severe("Unhandled transform: " + transform);
			}
		}
		return result;
	}

	@Override
	public void characters(final char[] ch, final int start, final int length) {
		if (inText) {
			String text = new String(ch, start, length).trim();
			if (!text.isEmpty()) {
				g.charpath(text);
			}
		}
	}
}
