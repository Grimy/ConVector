/*
   import javafx.scene.shape.Shape;
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

import java.io.IOException;
import java.io.StringReader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.Line;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Shear;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;

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

	private static final Set<String> defs = new HashSet<>(Arrays.asList(
				"defs", "symbol", "clipPath", "linearGradient"));

	private final Graphics g = new Graphics();
	private final Deque<String> idStack = new ArrayDeque<>();
	private final Map<String, Color> gradients = new HashMap<>();
	private final Map<String, Shape> paths = new HashMap<>();
	private Attributes attributes;
	private boolean inText;
	private boolean hasViewBox;

	private final Map<String, Consumer<String>> attrHandlers = new HashMap<>(); {
		attrHandlers.put("display", v -> {
			if (v.equals("none")) {
				g.clip(new Path());
			}
		});
		attrHandlers.put("fill", v -> parseColor(v, g::setFillColor));
		attrHandlers.put("fill-rule", v -> {
			if (v.equals("evenodd")) {
				g.setWindingRule(FillRule.EVEN_ODD);
			}
		});
		attrHandlers.put("clip-path", v -> getURL(paths, v, path -> g.clip(path)));
		attrHandlers.put("viewBox", v -> parseViewBox(v));
		attrHandlers.put("stroke", v -> parseColor(v, g::setStrokeColor));
		attrHandlers.put("stop-color", v -> parseColor(v, color ->
			gradients.put(idStack.peek(), color)));
		attrHandlers.put("transform", v -> parseTransform(v));
		attrHandlers.put("style", v -> Arrays.stream(v.split(";")).forEach(prop ->
			handleAttr(prop.split(":")[0], prop.split(":")[1])));
		attrHandlers.put("stroke-dasharray", v -> {/*TODO*/});
		attrHandlers.put("stroke-dashoffset", v -> {/*TODO*/});
		attrHandlers.put("stroke-width", v -> g.setStrokeWidth(parseLength(v)));
		attrHandlers.put("stroke-linecap", v -> g.setLineCap(
			StrokeLineCap.valueOf(v.toUpperCase())));
		attrHandlers.put("stroke-linejoin", v -> g.setLineJoin(
			StrokeLineJoin.valueOf(v.toUpperCase())));
		attrHandlers.put("stroke-miterlimit", v -> g.setMiterLimit(parseLength(v)));
		attrHandlers.put("font-size", v -> g.setFontSize(parseLength(v)));
		attrHandlers.put("font-family", v -> g.setFont(v));
		// font-weight, text-align, text-anchor
	}

	private final Map<String, Runnable> tagHandlers = new HashMap<>(); {
		tagHandlers.put("svg", () -> {
			if (!hasViewBox) {
				g.clip(new Rectangle(0, 0, getFloat("width", Float.MAX_VALUE),
					getFloat("height", Float.MAX_VALUE)));
			}
		});
		tagHandlers.put("use", () -> g.append(paths.getOrDefault(
			attributes.getValue("xlink:href"), new Path())));
		tagHandlers.put("line", () -> g.append(new Line(
			getFloat("x1", 0f), getFloat("y1", 0f),
			getFloat("x2", 0f), getFloat("y2", 0f))));
		tagHandlers.put("ellipse", () -> g.append(new Ellipse(
			getFloat("cx", 0f), getFloat("cy", 0f),
			getFloat("rx", getFloat("r", 0f)), getFloat("ry", getFloat("r", 0f)))));
		tagHandlers.put("circle", tagHandlers.get("ellipse"));
		tagHandlers.put("polygon", () -> parsePathData("M" + attributes.getValue("points") + "z"));
		tagHandlers.put("polyline", () -> parsePathData("M" + attributes.getValue("points")));
		tagHandlers.put("rect", () -> {
			final Rectangle rect = new Rectangle(getFloat("x", 0f), getFloat("y", 0f),
				getFloat("width", 0f), getFloat("height", 0f));
			final float rx = getFloat("rx", getFloat("ry", 0f)), ry = getFloat("ry", rx);
			rect.setArcWidth(2 * rx);
			rect.setArcHeight(2 * ry);
			g.append(rect);
		});
		tagHandlers.put("text", () -> {
			g.moveTo(getFloat("x", 0), getFloat("y", 0));
			inText = true;
		});
		tagHandlers.put("tspan", () -> g.moveTo(getFloat("x", Float.NaN), getFloat("y", Float.NaN)));
		tagHandlers.put("path", () -> parsePathData(attributes.getValue("d")));
		tagHandlers.put("linearGradient", () -> gradients.put(
			idStack.peek(), gradients.get(attributes.getValue("xlink:href"))));
		tagHandlers.put("radialGradient", () -> gradients.put(
			'#' + attributes.getValue("id"), null));
	}

	private static final Map<String, Function<Scanner, Transform>>
		transformHandlers = new HashMap<>(); static {
		transformHandlers.put("matrix", s -> new Affine(
			s.nextFloat(), s.nextFloat(), s.nextFloat(),
			s.nextFloat(), s.nextFloat(), s.nextFloat()));
		transformHandlers.put("translate", s -> new Translate(
			s.nextFloat(), s.hasNextFloat() ? s.nextFloat() : 0));
		transformHandlers.put("rotate", s -> new Rotate(
			s.nextFloat(),
			s.hasNextFloat() ? s.nextFloat() : 0,
			s.hasNextFloat() ? s.nextFloat() : 0));
		transformHandlers.put("scale", s -> {
			final float xScale = s.nextFloat();
			return new Scale(xScale,
				s.hasNextFloat() ? s.nextFloat() : xScale);
		});
		transformHandlers.put("skewX", s -> new Shear(
			Math.tan(Math.toRadians(s.nextFloat())), 0));
		transformHandlers.put("skewY", s -> new Shear(
			0, Math.tan(Math.toRadians(s.nextFloat()))));
	}

	private static <T> void getURL(final Map<String, T> map, final String url,
			final Consumer<T> sink) {
		final String id = url.substring(4, url.length() - 1);
		if (map.containsKey(id)) {
			sink.accept(map.get(id));
		}
	}

	private void handleAttr(final String name, final String value) {
		if (!value.equals("inherit")) {
			attrHandlers.getOrDefault(name.trim(), v -> {/*NOOP*/}).accept(value.trim());
		}
	}

	@Override
	public Graphics process(final ReadableByteChannel input) {
		g.setFillColor(Color.BLACK);
		g.setStrokeColor(null);
		g.setFont("DejaVu Serif");
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
		// TODO handle % correctly
		return multiplier == null ? Float.parseFloat(floatString.replace("%", ""))
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
		if (defs.contains(name) || !idStack.isEmpty()) {
			idStack.push('#' + attributes.getValue("id"));
		}
		for (int i = 0; i < attributes.getLength(); i++) {
			handleAttr(attributes.getLocalName(i), attributes.getValue(i));
		}
		tagHandlers.getOrDefault(name, () -> {/*NOOP*/}).run();
	}

	@Override
	public void endElement(final String namespace, final String local, final String name) {
		if (idStack.isEmpty()) {
			g.fill().stroke().resetPath();
		} else {
			idStack.pop();
			// paths.put(idStack.pop(), g.getPath());
			if (defs.contains(name)) {
				g.resetPath();
			}
		}
		inText &= !name.equals("text");
		g.restore();
	}

	private void parsePathData(final String data) {
		final SVGPath path = new SVGPath();
		path.setContent(data);
		g.append(path);
	}

	private void parseColor(final String colorName, final Consumer<Color> callback) {
		if (colorName.startsWith("url(")) {
			getURL(gradients, colorName, callback);
			return;
		}
		if (colorName.equals("currentColor")) {
			// TODO
			return;
		}
		if (colorName.equals("none")) {
			callback.accept(null);
			return;
		}
		callback.accept(Color.web(colorName));
	}

	private void parseTransform(final String transform) {
		@SuppressWarnings("resource")
		final Scanner scanner = new Scanner(transform);
		scanner.useDelimiter("[(,\\s)]*(?:[(,\\s)]|(?<![eE])(?=[-+]))");
		scanner.forEachRemaining(token -> g.transform(
			transformHandlers.get(token).apply(scanner)));
	}

	private void parseViewBox(final String viewBox) {
		@SuppressWarnings("resource")
		final Scanner scanner = new Scanner(viewBox);
		g.transform(new Translate(-scanner.nextFloat(), -scanner.nextFloat()));
		g.clip(new Rectangle(0, 0, scanner.nextFloat(), scanner.nextFloat()));
		hasViewBox = true;
	}

	@Override
	public void characters(final char[] ch, final int start, final int length) {
		if (inText) {
			final String text = new String(ch, start, length).trim();
			if (!text.isEmpty()) {
				g.charpath(text);
			}
		}
	}
}
