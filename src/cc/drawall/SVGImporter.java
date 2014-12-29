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

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
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

public class SVGImporter extends DefaultHandler implements Importer {
	private static final Logger log = Logger.getLogger(SVGImporter.class.getName());

	private static final Pattern SVG_COMMAND = Pattern.compile("[mzlhvcsqtaMZLHVCSQTA]");

	private enum Caps {
		BUTT, ROUND, SQUARE;
	}

	private enum Joins {
		MITER, ROUND, BEVEL;
	}

	private WriterGraphics2D g;

	private final Map<String, Consumer<String>> attrHandlers = new HashMap<>(); {
		attrHandlers.put("fill", v -> g.setFillColor(parseColor(v)));
		attrHandlers.put("stroke", v -> g.setColor(parseColor(v)));
		attrHandlers.put("transform", v -> g.getTransform().concatenate(parseTransform(v)));
		attrHandlers.put("style", v -> Arrays.stream(v.split(";")).forEach(prop -> handleAttr(
				prop.split(":")[0], prop.split(":")[1])));
		attrHandlers.put("stroke-width", v -> g.setStrokeWidth(Float.parseFloat(v.replace("px", ""))));
		attrHandlers.put("stroke-linecap", v -> g.setLineCap(Caps.valueOf(v.toUpperCase()).ordinal()));
		attrHandlers.put("stroke-linejoin", v -> g.setLineJoin(Joins.valueOf(v.toUpperCase()).ordinal()));
		attrHandlers.put("stroke-miterlimit", v -> g.setMiterLimit(Float.parseFloat(v)));
	}

	private void handleAttr(final String name, final String value) {
		attrHandlers.getOrDefault(name, v -> log.finest(v)).accept(value);
	}


	@Override
	public void process(final InputStream input, final WriterGraphics2D output) {
		this.g = output;
		try {
			SAXParserFactory.newInstance().newSAXParser().parse(input, this);
		} catch (final ParserConfigurationException | IOException e) {
			assert false : "XML error : " + e;
		} catch (final SAXException e) {
			log.severe("Input is not a valid SVG");
			log.finer(e.toString());
		}
	}

	@Override
	public InputSource resolveEntity(final String publicId, final String systemId) {
		return new InputSource(SVGImporter.class.getResourceAsStream("dtd/svg10.dtd"));
	}

	public float getFloat(final Attributes attr, final String name, final float def) {
		final String value = attr.getValue(name);
		return value != null ? Float.parseFloat(value) : def;
	}

	@Override
	public void startElement(final String namespace, final String local,
			final String name, final Attributes attr) {
		g.save();
		for (int i = 0; i < attr.getLength(); i++) {
			handleAttr(attr.getLocalName(i), attr.getValue(i));
		}

		String d = null;
		switch (name) {
		case "svg":
			g.append(new Rectangle2D.Float(0, 0, getFloat(attr, "width", Float.MAX_VALUE),
						getFloat(attr, "height", Float.MAX_VALUE)));
			g.clip();
			g.reset();
			break;
		case "line":
			d = "M " + attr.getValue("x1") + "," + attr.getValue("y1")
			 + " L " + attr.getValue("x2") + "," + attr.getValue("y2");
			break;
		case "ellipse":
			float rx = getFloat(attr, "rx", 0f);
			float ry = getFloat(attr, "ry", 0f);
			float cx = getFloat(attr, "cx", 0f);
			float cy = getFloat(attr, "cy", 0f);
			d = "M " + (cx - rx) + "," + (cy - ry)
				+ "A" + rx + "," + ry + " 0 1 0 " + (cx + rx) + "," + (cy + ry)
				+ "A" + rx + "," + ry + " 0 1 0 " + (cx - rx) + "," + (cy - ry);
			break;
		case "polygon":
			d = "M" + attr.getValue("points").replace(' ', 'L');
			break;
		case "rect":
		case "circle":
		case "polyline":
			log.severe("Unhandled basic shape: " + name);
			break;
		default:
			d = attr.getValue("d");
		}
		if (d != null) {
			parsePathData(d);
			g.draw();
		}
	}

	@Override
	public void endElement(final String namespace, final String local, final String name) {
		g.restore();
	}

	private void parsePathData(final String data) {
		@SuppressWarnings("resource")
		final Scanner scanner = new Scanner(data);
		scanner.useDelimiter("(?<=[mzlhvcsqtaMZLHVCSQTA])\\s*|" +
				"[\\s,]*(?:[\\s,]|(?=[^\\deE.-])|(?<![eE])(?=-))");
		char cmd = '!';

		while (scanner.hasNext()) {
			if (scanner.hasNext(SVG_COMMAND)) {
				cmd = scanner.next(SVG_COMMAND).charAt(0);
			}
			log.finest("cmd: " + cmd);
			final boolean relative = Character.isLowerCase(cmd);
			if (relative && g.getCurrentPoint() == null) {
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
				g.quadTo(relative, scanner.nextFloat(), scanner.nextFloat(),
						scanner.nextFloat(), scanner.nextFloat());
				break;
			case 'C':
				g.curveTo(relative, scanner.nextFloat(), scanner.nextFloat(),
						scanner.nextFloat(), scanner.nextFloat(),
						scanner.nextFloat(), scanner.nextFloat());
				break;
			case 'Z':
				g.closePath();
				cmd = '!';
				break;
			case 'A':
				g.arcTo(relative, new Point2D.Float(scanner.nextFloat(), scanner.nextFloat()),
						scanner.nextFloat(), scanner.nextInt() != 0, scanner.nextInt() != 0,
						scanner.nextFloat(), scanner.nextFloat());
				break;
			case 'H': // horizontal
				g.lineTo(relative, scanner.nextFloat(), Float.NaN);
				break;
			case 'V': // vertical
				g.lineTo(relative, Float.NaN, scanner.nextFloat());
				break;
			case 'T': // smooth quadratic
				Point2D p = g.getCurrentPoint();
				g.quadTo(relative, Float.NaN, Float.NaN,
						scanner.nextFloat(), scanner.nextFloat());
				break;
			case 'S': // smooth cubic
				p = g.getCurrentPoint();
				g.curveTo(relative, Float.NaN, Float.NaN,
						scanner.nextFloat(), scanner.nextFloat(),
						scanner.nextFloat(), scanner.nextFloat());
				break;
			default:
				assert false;
			}
		}
	}

	private static Color parseColor(final String colorName) {
		if ("none".equals(colorName)) {
			return null;
		}
		javafx.scene.paint.Color color = javafx.scene.paint.Color.web(colorName);
		return new Color((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue());
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
}
