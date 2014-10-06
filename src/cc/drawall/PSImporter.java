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
import java.awt.Font;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.xml.bind.DatatypeConverter;

/** Importer used to parse PostScript. */
public class PSImporter implements Importer {
	private static final Logger log = Logger.getLogger(PSImporter.class.getName());

	/** PostScript® is a trademark of Adobe Systems Incorporated. */

	/** The correspondance between PostScript type and Java types is as follow:
	  * Operator    Runnable
	  * Real        Float
	  * Integer     Float
	  * Boolean     Boolean
	  * Array       Object[]
	  * Dictionary  Map<Object, Object>
	  * Name        String
	  * String      String
	  **/

	/** A Runnable that does nothing, used for ignored instructions. */
	private static final Runnable NOOP = () -> log.finest("No-op");

	/* ==PATTERNS== */
	private static final Pattern NUMBER = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?");
	private static final Pattern STRING = Pattern.compile("(?:\\\\.|[^()])*");
	private static final Pattern HEX_STRING = Pattern.compile("[0-9a-fA-F\000\t\r\n\f ]*");

	/* ==STACKS==
	 * The PostScript interpreter manages several stacks. See PLRM 3.4: Stacks.
	 * Here, some of those stacks are implemented by intrusive linked lists:
	 * each item links to the item underneath it on the stack.
	 * `null` represents an empty stack. */

	/** Operand stack. */
	private final Stack<Object> stack = new Stack<>();

	private static final Object MARK = new Object();
	private static final Object CURLY_MARK = new Object();

	private WriterGraphics2D g;

	private final Map<Object, Void> literals = new IdentityHashMap<>();
	private Iterator<Object> itr;

	/** Dictionary stack. */
	private final Map<Object, Object> vars = new HashMap<>(); {
		// The categories and their order are from PLRM3 8.1: Operator Summary
		// Stack manipulation
		builtin("pop", () -> stack.pop());
		builtin("exch", () -> Collections.rotate(substack(2), 1));
		builtin("dup", () -> stack.push(stack.peek()));
		// copy
		builtin("index", () -> stack.push(stack.get(stack.size() - (int) p(1))));
		builtin("roll", () -> Collections.rotate(substack((int) p(2)), (int) p()));
		builtin("clear", () -> stack.clear());
		// count, mark, cleartomark, counttomark (not in PDF)

		// Math
		builtin("add", () -> stack.push(p(2) + p()));
		builtin("div", () -> stack.push(p(2) / p()));
		// idiv, mod
		builtin("mul", () -> stack.push(p(2) * p()));
		builtin("sub", () -> stack.push(p(2) - p()));
		builtin("abs", () -> stack.push(Math.abs(p(1))));
		builtin("neg", () -> stack.push(-p(1)));
		// ceiling, floor, round, truncate, sqrt, atan, cos, sin, exp, ln, log
		// rand, srand, rrand

		// Array
		// array
		vars.put("[", MARK);
		builtin("]", () -> stack.push(popTo(MARK)));
		builtin("astore", () -> {
			final Object[] array = (Object[]) stack.pop();
			stack.push(substack(array.length).toArray(array));
		});
		builtin("length", () -> stack.push(stack.pop() instanceof Map ? 0.0f : 0.0f));

		// Dictionary
		vars.put("systemdict", vars);
		vars.put("currentdict", vars);
		builtin("dict", () -> stack.set(0, vars));
		builtin("begin", () -> stack.pop());
		builtin("end", NOOP);
		builtin("load", () -> stack.push(getVar(stack.pop())));
		builtin("def", () -> vars.put(pop2(), stack.pop()));
		builtin("get", () -> {
			Object o = pop2();
			stack.push(o instanceof Map ? ((Map<?, ?>) o).get(stack.pop()) : ((Object[]) o)[(int) p(1)]);
		});
		builtin("known", () -> stack.push(((Map<?, ?>) pop2()).containsKey(stack.pop())));
		builtin("where", () -> stack.push(Boolean.FALSE));
		builtin("cleardictstack", NOOP);

		// String (TODO?)

		// Relational, boolean and bitwise
		builtin("eq", () -> stack.push(p(2) == p())); // use .equals instead?
		builtin("ne", () -> stack.push(p(2) != p()));
		builtin("gt", () -> stack.push(compare() < 0));
		builtin("lt", () -> stack.push(compare() > 0));
		// ge, lt, le, and, or, xor
		builtin("not", () -> stack.push(!(Boolean) stack.pop()));
		// bitshift
		vars.put("true", Boolean.TRUE);
		vars.put("false", Boolean.FALSE);

		// Flow control
		builtin("exec", () -> execute(stack.pop()));
		builtin("quit", NOOP);
		builtin("if", () -> {
			final Object code = stack.pop();
			if ((boolean) stack.pop()) {
				execute(code);
			}
		});
		builtin("ifelse", () -> {
			final Object ifFalse = stack.pop();
			final Object ifTrue = stack.pop();
			execute((boolean) stack.pop() ? ifTrue : ifFalse);
		});
		builtin("repeat", () -> {
			final Object code = stack.pop();
			for (int i = (int) p(1); i > 0; i--) {
				execute(code);
			}
		});
		builtin("for", () -> {
			final Object code = stack.pop();
			for (float i = p(3), inc = p(), max = p(); i < max; i += inc) {
				stack.push(i);
				execute(code);
			}
		});
		builtin("forall", () -> {
			final Object code = stack.pop();
			for (Object o: stack.peek() instanceof Font ? new Object[0] : (Object[]) stack.pop()) {
				stack.push(o);
				execute(code);
			}
		});
		// exec

		// Type, attributes and conversion operators
		// cvi, cvr
		builtin("readonly", NOOP);

		// File operators
		builtin("==", () -> System.out.println(stack.pop()));
		builtin("stack", () -> stack.stream().forEach(System.out::println));

		// Miscellaneous
		builtin("bind", () -> stack.push(Arrays.stream((Object[]) stack.pop()).map(
			o -> vars.containsKey(o) ? vars.get(o) : o
		).toArray()));

		// Graphics State
		builtin("gsave", () -> g.save());
		builtin("grestore", () -> g.restore());
		builtin("setlinecap",    () -> g.setStrokeCap(popFlag()));
		builtin("setlinejoin",   () -> g.setStrokeJoin(popFlag()));
		builtin("setlinewidth",  () -> g.setStrokeWidth(p(1)));
		builtin("setmiterlimit", () -> g.setStrokeMiterLimit(p(1)));
		builtin("setdash",       () -> g.setStrokeDash(popArray(), p(1)));
		builtin("showpage", NOOP);
		builtin("setrgbcolor", () -> g.setColor(new Color(p(3), p(), p())));
		builtin("sethsbcolor", () -> g.setColor(Color.getHSBColor(p(3), p(), p())));
		builtin("setcmykcolor", () -> stack.push(new Color(
				ColorSpace.getInstance(ColorSpace.TYPE_CMYK),
				new float[] {p(4), p(), p(), p()}, 1.0f)));
		builtin("setcmykcolor", () -> substack(4).clear());
		builtin("setgray", () -> g.setColor(new Color((int) (0xFFFFFF * p(1)))));
		builtin("clippath", () -> {
			g.reset();
			g.append(g.getClip());
		});
		builtin("pathbbox", () -> {
			Rectangle2D r = g.getBounds();
			stack.push(r.getMinX());
			stack.push(r.getMinY());
			stack.push(r.getMaxX());
			stack.push(r.getMaxY());
		});

		// Coordinate systems
		builtin("matrix", () -> stack.push(new float[]{1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f}));
		builtin("currentmatrix", () -> log.severe("Unsupported operator: currentmatrix"));
		builtin("setmatrix", () -> g.getTransform().setTransform(popMatrix()));
		builtin("concat", () -> g.getTransform().concatenate(popMatrix()));
		builtin("rotate", () -> g.getTransform().rotate(Math.toRadians(p(1))));
		builtin("scale", () -> g.getTransform().scale(p(2), p()));
		builtin("translate", () -> g.getTransform().translate(p(2), p()));

		// Path construction
		builtin("newpath", () -> g.reset());
		builtin("moveto", () -> g.moveTo(false, p(2), p()));
		builtin("rmoveto", () -> g.moveTo(true, p(2), p()));
		builtin("lineto", () -> g.lineTo(false, p(2), p()));
		builtin("rlineto", () -> g.lineTo(true, p(2), p()));
		builtin("curveto", () -> g.curveTo(false, p(6), p(), p(), p(), p(), p()));
		builtin("closepath", () -> g.closePath());
		builtin("currentpoint", () -> {
			final Point2D point = g.getCurrentPoint();
			stack.push(point.getX());
			stack.push(point.getY());
		});

		// Painting
		builtin("stroke", () -> g.draw());
		builtin("fill", () -> g.fill(Path2D.WIND_NON_ZERO));
		builtin("eofill", () -> g.fill(Path2D.WIND_EVEN_ODD));
		builtin("clip", () -> g.clip());

		// Insideness-testing (TODO?)

		// Glyph and font
		builtin("definefont", () -> vars.put(pop2(), stack.peek()));
		builtin("findfont", () -> stack.push(new Font(
				((String) stack.pop()).replace('-', ' '), 0, 1)));
		builtin("scalefont", () -> {
			final float scale = p(1);
			final Font font = (Font) stack.pop();
			final AffineTransform transform = font.getTransform();
			transform.scale(scale, scale);
			stack.push(font.deriveFont(transform));
		});
		builtin("setfont", () -> {
			log.fine("Setting font : " + stack.peek());
			g.setFont((Font) stack.pop());
		});
		builtin("show", () -> g.drawString((String) stack.pop()));
		builtin("ashow", () -> {
			g.drawString((String) stack.pop());
			p(2);
			p();
		});
		builtin("charpath", () -> {
			stack.pop(); // TODO boolean stroke
			g.charpath((String) stack.pop());
		});

		builtin("save", () -> stack.push(null));
		builtin("restore", () -> stack.pop());
	}

	private int compare() {
		@SuppressWarnings("unchecked")
		final Comparable<Object> top = (Comparable<Object>) stack.pop();
		return top.compareTo(stack.pop());
	}

	@Override
	public void process(final InputStream input, final WriterGraphics2D out) {
		@SuppressWarnings("resource")
		final Scanner scanner = new Scanner(input, "ascii");
		g = out;
		g.getClip().intersect(new Area(new Rectangle2D.Float(0, 0, 612, 792)));
		g.getTransform().scale(1, -1);
		g.getTransform().translate(0, -792);

		// See PLRM 3.1: Syntax
		final String delimiters = "[(){}<>\\[\\]/]";
		scanner.useDelimiter("([\000\t\r\n\f ]|(?=" + delimiters + ")|(?<=" + delimiters + ")|%.*+)+");
		while (scanner.hasNext()) {
			final Object obj = tokenize(scanner.next(), scanner);
			if (stack.contains(CURLY_MARK)) {
				// Deferred execution mode
				stack.push(obj);
			} else {
				execute(obj);
			}
		}
	}

	private void execute(final Object object) {
		if (literals.containsKey(object)) {
			stack.push(object);
		} else if (object instanceof Runnable) {      // built-in operator
			((Runnable) object).run();
		} else if (object instanceof String) { // name object
			execute(getVar(object));
		} else if (object instanceof Object[]) {   // procedure
			Arrays.stream((Object[]) object).forEach(this::execute);
		} else {
			stack.push(object);
		}
	}

	/** Process a single input token. */
	private Object tokenize(final String token, final Scanner scanner) {
		switch (token.charAt(0)) {
		case '(':
			final String string = scanner.findWithinHorizon(STRING, 0);
			scanner.next();
			return literal(string.replaceAll("\\\\(?!\\\\)", ""));
		case '<':
			final String hexString = scanner.findWithinHorizon(HEX_STRING, 0).replaceAll("[\r\n ]", "");
			scanner.next();
			return literal(DatatypeConverter.parseHexBinary(hexString));
		case '{':
			return CURLY_MARK;
		case '}':
			final Object[] proc = popTo(CURLY_MARK);
			return (Runnable) () -> stack.push(proc);
		case '/':
			return literal(scanner.next());
		default:
			return NUMBER.matcher(token).matches() ? Float.valueOf(token) : token;
		}
	}

	private Object literal(final Object obj) {
		literals.put(obj, null);
		return obj;
	}

	///////////////////////
	// Dict manipulation //
	///////////////////////

	private void builtin(final String key, final Runnable operation) {
		vars.put(key, operation);
	}

	private Object getVar(final Object key) {
		assert vars.containsKey(key) : "Unknown variable : " + key;
		return vars.get(key);
	}

	/////////////////////////
	// Stack manipualation //
	/////////////////////////

	private float p(final int count) {
		itr = new ArrayList<>(substack(count)).iterator();
		stack.setSize(stack.size() - count);
		return p();
	}

	private float p() {
		return (Float) itr.next();
	}

	private Object pop2() {
		final Object fst = stack.pop();
		final Object snd = stack.pop();
		stack.push(fst);
		return snd;
	}

	/** Pops n items from the stack and returns them as an array. */
	private Object[] popTo(final Object mark) {
		int n = stack.lastIndexOf(mark);
		assert n >= 0 : "No matching mark";
		final Object[] array = substack(stack.size() - 1 - n).toArray();
		stack.setSize(n);
		return array;
	}

	/** Returns a live view of the top n elements of the stack. */
	private List<Object> substack(int n) {
		final int size = stack.size();
		return stack.subList(size - n, size);
	}

	private int popFlag() {
		final float val = p(1);
		assert val >= 0 && val < 3;
		return (int) val;
	}

	private float[] popArray() {
		final Object[] array = (Object[]) stack.pop();
		final float[] result = new float[array.length];
		for (int i = 0; i < array.length; i++) {
			result[i] = (Float) array[i];
		}
		return result;
	}

	private AffineTransform popMatrix() {
		final float[] matrix = popArray();
		assert matrix.length == 6;
		return new AffineTransform(matrix);
	}
}
