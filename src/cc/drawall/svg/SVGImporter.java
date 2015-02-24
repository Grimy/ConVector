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
import java.awt.geom.RoundRectangle2D;
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

	private final Map<String, Float> unitMap = new HashMap<>(); {
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
	private final Map<String, Path2D> paths = new HashMap<>();
	private Attributes attributes;
	private boolean inText;

	private final Map<String, Consumer<String>> attrHandlers = new HashMap<>(); {
		attrHandlers.put("display", v -> {
			if (v.equals("none")) {
				g.clip(new Path2D.Float());
			}
		});
		attrHandlers.put("fill", v -> parseColor(v, g::setFillColor));
		attrHandlers.put("fill-rule", v -> {
			if (v.equals("evenodd")) {
				g.setWindingRule(Path2D.WIND_EVEN_ODD);
			}
		});
		attrHandlers.put("clip-path", v -> getURL(paths, v, path ->
			g.clip(g.getTransform().createTransformedShape(path))));
		attrHandlers.put("viewBox", v -> parseViewBox(v));
		attrHandlers.put("stroke", v -> parseColor(v, g::setStrokeColor));
		attrHandlers.put("stop-color", v -> parseColor(v, color ->
			gradients.put(idStack.peek(), color)));
		attrHandlers.put("transform", v -> parseTransform(v));
		attrHandlers.put("style", v -> Arrays.stream(v.split(";")).forEach(prop ->
			handleAttr(prop.split(":")[0], prop.split(":")[1])));
		attrHandlers.put("stroke-dasharray", v -> g.setDashArray(parseArray(v)));
		attrHandlers.put("stroke-dashoffset", v -> g.setDashOffset(Float.parseFloat(v)));
		attrHandlers.put("stroke-width", v -> g.setStrokeWidth(parseLength(v, '/')));
		attrHandlers.put("stroke-linecap", v -> g.setLineCap(
			Graphics.LineCap.valueOf(v.toUpperCase())));
		attrHandlers.put("stroke-linejoin", v -> g.setLineJoin(
			Graphics.LineJoin.valueOf(v.toUpperCase())));
		attrHandlers.put("stroke-miterlimit", v -> g.setMiterLimit(parseLength(v, '/')));
		attrHandlers.put("font-size", v -> g.setFontSize(parseLength(v, '/')));
		attrHandlers.put("font-family", v -> g.setFont(v));
		// font-weight, text-align, text-anchor
	}

	private static final float sqLen(float x, float y) {
		return x * x + y * y;
	}

	private final Map<String, Runnable> tagHandlers = new HashMap<>(); {
		tagHandlers.put("svg", () -> {
			unitMap.putIfAbsent("%w", getFloat("width", Float.MAX_VALUE) / 100);
			unitMap.putIfAbsent("%h", getFloat("height", Float.MAX_VALUE) / 100);
			unitMap.put("%/", (float) Math.sqrt(sqLen(unitMap.get("%w"), unitMap.get("%h")) / 2));
			g.clip(new Rectangle2D.Float(0, 0, 100 * unitMap.get("%w"),
				100 * unitMap.get("%h")));
		});
		tagHandlers.put("use", () -> g.append(paths.getOrDefault(
			attributes.getValue("xlink:href"), new Path2D.Float())));
		tagHandlers.put("line", () -> {
			g.moveTo(getFloat("x1", 0f), getFloat("y1", 0f));
			g.lineTo(getFloat("x2", 0f), getFloat("y2", 0f));
		});
		tagHandlers.put("ellipse", () -> {
			final float rx = getFloat("rx", getFloat("r", 0f));
			final float ry = getFloat("ry", getFloat("r", 0f));
			final float cx = getFloat("cx", 0f);
			final float cy = getFloat("cy", 0f);
			g.append(g.getTransform().createTransformedShape(new Ellipse2D.Float(
				cx - rx, cy - ry, 2 * rx, 2 * ry)));
		});
		tagHandlers.put("circle", tagHandlers.get("ellipse"));
		tagHandlers.put("polygon", () -> parsePathData(attributes.getValue("points") + "z"));
		tagHandlers.put("polyline", () -> parsePathData(attributes.getValue("points")));
		tagHandlers.put("rect", () -> {
			final float x = getFloat("x", 0f), y = getFloat("y", 0f);
			final float width = getFloat("width", 0f), height = getFloat("height", 0f);
			final float rx = getFloat("rx", getFloat("ry", 0f)), ry = getFloat("ry", rx);
			g.append(g.getTransform().createTransformedShape(rx == 0 && ry == 0
				? new Rectangle2D.Float(x, y, width, height)
				: new RoundRectangle2D.Float(x, y, width, height, 2 * rx, 2 * ry)));
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

	private final Map<Character, Consumer<Scanner>> pathHandlers = new HashMap<>(); {
		pathHandlers.put('M', s -> g.moveTo(s.nextFloat(), s.nextFloat()));
		pathHandlers.put('L', s -> g.lineTo(s.nextFloat(), s.nextFloat()));
		pathHandlers.put('Q', s -> g.lineTo(s.nextFloat(), s.nextFloat(),
			s.nextFloat(), s.nextFloat()));
		pathHandlers.put('C', s -> g.lineTo(s.nextFloat(), s.nextFloat(),
			s.nextFloat(), s.nextFloat(), s.nextFloat(), s.nextFloat()));
		pathHandlers.put('Z', s -> {
			g.closePath();
			if (idStack.isEmpty() && g.getFillColor() == null) {
				g.stroke().resetKeepPos();
			}
		});
		pathHandlers.put('A', s -> {
			final float rx = s.nextFloat(), ry = s.nextFloat();
			g.arcTo(s.nextFloat(), s.nextInt() != 0, s.nextInt() != 0,
				rx, ry, s.nextFloat(), s.nextFloat());
		});
		pathHandlers.put('H', s -> g.lineTo(s.nextFloat(), Float.NaN));
		pathHandlers.put('V', s -> g.lineTo(Float.NaN, s.nextFloat()));
		pathHandlers.put('T', s -> g.lineTo(Float.NaN, Float.NaN,
			s.nextFloat(), s.nextFloat()));
		pathHandlers.put('S', s -> g.lineTo(Float.NaN, Float.NaN,
			s.nextFloat(), s.nextFloat(), s.nextFloat(), s.nextFloat()));
	}

	private static final Map<String, Function<Scanner, AffineTransform>>
		transformHandlers = new HashMap<>(); static {
		transformHandlers.put("matrix", s -> new AffineTransform(
			s.nextFloat(), s.nextFloat(), s.nextFloat(),
			s.nextFloat(), s.nextFloat(), s.nextFloat()));
		transformHandlers.put("translate", s -> AffineTransform.getTranslateInstance(
			s.nextFloat(), s.hasNextFloat() ? s.nextFloat() : 0));
		transformHandlers.put("rotate", s -> AffineTransform.getRotateInstance(
			Math.toRadians(s.nextFloat()),
			s.hasNextFloat() ? s.nextFloat() : 0,
			s.hasNextFloat() ? s.nextFloat() : 0));
		transformHandlers.put("scale", s -> {
			final float xScale = s.nextFloat();
			return AffineTransform.getScaleInstance(xScale,
				s.hasNextFloat() ? s.nextFloat() : xScale);
		});
		transformHandlers.put("skewX", s -> AffineTransform.getShearInstance(
			Math.tan(Math.toRadians(s.nextFloat())), 0));
		transformHandlers.put("skewY", s -> AffineTransform.getShearInstance(
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
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setNamespaceAware(true);
			factory.newSAXParser().parse(Channels.newInputStream(input), this);
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
	private float parseLength(final String floatString, final char dim) {
		if (fontSizes.containsKey(floatString)) {
			return fontSizes.get(floatString);
		}
		String str = floatString.endsWith("%") ? floatString + dim : floatString;
		final int index = str.length() - 2;  // all SVG unitMap are 2 chars long
		final Float multiplier = index < 0 ? null : unitMap.get(str.substring(index));
		float result = multiplier == null ? Float.parseFloat(str)
			: multiplier * Float.parseFloat(str.substring(0, index));
		return result;
	}

	private float getFloat(final String name, final float def) {
		final String value = attributes.getValue(name);
		final char dim = name.contains("x") || name.equals("width") ? 'w'
			: name.contains("y") || name.equals("height") ? 'h' : '/';
		return value == null ? def : parseLength(value.trim().split(" ")[0], dim);
	}

	@Override
	public void startElement(final String uri, final String name,
			final String qname, final Attributes attributes) {
		g.setRelative(false).save();
		if (!uri.equals("http://www.w3.org/2000/svg")) {
			return;
		}
		this.attributes = attributes;
		for (int i = 0; i < attributes.getLength(); i++) {
			handleAttr(attributes.getLocalName(i), attributes.getValue(i));
		}
		if (defs.contains(name) || !idStack.isEmpty()) {
			idStack.push('#' + attributes.getValue("id"));
		}
		tagHandlers.getOrDefault(name, () -> {/*NOOP*/}).run();
	}

	@Override
	public void endElement(final String namespace, final String local, final String name) {
		if (idStack.isEmpty()) {
			g.fill().stroke().resetPath();
		} else {
			paths.put(idStack.pop(), g.getPath());
			if (defs.contains(name)) {
				g.resetPath();
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
		char cmd = 'M';
		while (scanner.hasNext()) {
			if (scanner.hasNext(SVG_COMMAND)) {
				cmd = scanner.next(SVG_COMMAND).charAt(0);
			}
			g.setRelative(Character.isLowerCase(cmd) && g.getCurrentPoint() != null);
			pathHandlers.get(Character.toUpperCase(cmd)).accept(scanner);
			if (Character.toUpperCase(cmd) == 'M') {
				--cmd;
			}
		}
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
		final javafx.scene.paint.Color color = javafx.scene.paint.Color.web(colorName);
		callback.accept(new Color((float) color.getRed(),
					(float) color.getGreen(), (float) color.getBlue()));
	}

	private void parseTransform(final String transform) {
		@SuppressWarnings("resource")
		final Scanner scanner = new Scanner(transform);
		scanner.useDelimiter("[(,\\s)]*(?:[(,\\s)]|(?<![eE])(?=[-+]))");
		scanner.forEachRemaining(token -> g.getTransform().concatenate(
			transformHandlers.get(token).apply(scanner)));
	}

	private void parseViewBox(final String viewBox) {
		@SuppressWarnings("resource")
		final Scanner scanner = new Scanner(viewBox);
		g.getTransform().translate(-scanner.nextFloat(), -scanner.nextFloat());
		unitMap.put("%w", scanner.nextFloat() / 100);
		unitMap.put("%h", scanner.nextFloat() / 100);
	}

	private static float[] parseArray(final String array) {
		if (array.equals("none")) {
			return null;
		}
		String[] tokens = array.split("[,\\s]+");
		float[] result = new float[tokens.length];
		float sum = 0;
		for (int i = 0; i < tokens.length; ++i) {
			sum += (result[i] = Float.parseFloat(tokens[i]));
		}
		return sum == 0 ? null : result;
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
