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

package cc.drawall.ps;

import java.awt.Color;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.InputMismatchException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import cc.drawall.Graphics;
import cc.drawall.Importer;

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
	  * Dictionary  PSDict
	  * Name        String
	  * String      String
	  **/

	/** A Runnable that does nothing, used for ignored instructions. */
	private static final Runnable NOOP = () -> log.finest("No-op");

	/* ==PATTERNS== */
	private static final Pattern NUMBER = Pattern.compile("[-+]?\\d*\\.?\\d+([eE][-+]?\\d+)?");
	private static final Pattern STRING = Pattern.compile("(?:\\\\.|[^()])*");
	private static final String WHITESPACE = "[\0\t\r\n\f ]";
	private static final Pattern HEX_STRING = Pattern.compile("(?:[0-9a-fA-F]|" + WHITESPACE + ")*");

	/* ==STACKS==
	 * The PostScript interpreter manages several stacks. See PLRM 3.4: Stacks.
	 * Here, some of those stacks are implemented by intrusive linked lists:
	 * each item links to the item underneath it on the stack.
	 * `null` represents an empty stack. */

	/** Operand stack. */
	private final Stack<Object> stack = new Stack<>();

	private static final Object MARK = new Object();
	private static final Object CURLY_MARK = new Object();

	private final Graphics g = new Graphics();

	private final Map<Object, Void> literals = new IdentityHashMap<>();
	private Iterator<Object> itr;

	/** Main dictionary. */
	private final PSDict vars = new PSDict(); {
		// The categories and their order are from PLRM3 8.1: Operator Summary
		// Stack manipulation
		builtin("pop", () -> stack.pop());
		builtin("exch", () -> Collections.rotate(substack(2), 1));
		builtin("dup", () -> stack.push(stack.peek()));
		builtin("copy", () -> {
			if (stack.peek() instanceof Double) {
				stack.addAll(new ArrayList<>(substack((int) p(1))));
			} else {
				pop2();
			}
		});
		builtin("index", () -> stack.push(stack.get(-(int) p(1) + stack.size())));
		builtin("roll", () -> Collections.rotate(substack((int) p(2)), (int) p()));
		builtin("clear", () -> stack.clear());
		builtin("cleartomark", () -> popTo(MARK));
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
		builtin("array", () -> stack.push(literal(new Object[(int) p(1)])));
		vars.put("[", MARK);
		builtin("]", () -> stack.push(literal(popTo(MARK))));
		builtin("astore", () -> {
			final Object[] array = (Object[]) stack.pop();
			stack.push(substack(array.length).toArray(array));
		});
		builtin("length", () -> {
			final Object o = stack.pop();
			stack.push(o instanceof Object[] ? ((Object[]) o).length : 0f);
		});

		// Dictionary
		vars.put("$error", vars);
		vars.put("errordict", vars);
		vars.put("userdict", vars);
		vars.put("statusdict", vars);
		vars.put("systemdict", vars);
		vars.put("currentdict", vars);
		vars.put("currentsystemparams", vars);
		builtin("countdictstack", () -> stack.push(1f));
		builtin("dictstack", () -> ((Object[]) stack.peek())[0] = vars);

		builtin("dict", () -> stack.push(p(1) > 0 ? vars : null));
		builtin("begin", () -> stack.pop());
		builtin("end", NOOP);
		builtin("load", () -> stack.push(getVar(stack.pop())));
		builtin("def", () -> vars.put(pop2(), stack.pop()));
		builtin("get", () -> {
			final Object o = pop2();
			stack.push(o instanceof PSDict ? ((PSDict) o).get(stack.pop())
					: o instanceof Object[] ? ((Object[]) o)[(int) p(1)]
					: ((String) o).codePointAt((int) p(1)));
		});
		builtin("put", () -> {
			final Object value = stack.pop();
			final Object o = pop2();
			if (o instanceof PSDict) {
				((PSDict) o).put(stack.pop(), value);
			} else {
				((Object[]) o)[(int) p(1)] = value;
			}
		});
		builtin("known", () -> stack.push(((PSDict) pop2()).containsKey(stack.pop())));
		builtin("where", () -> {
			final boolean exists = vars.containsKey(stack.pop());
			if (exists) {
				stack.push(vars);
			}
			stack.push(exists);
		});
		builtin("cleardictstack", NOOP);

		// String
		builtin("string", () -> stack.push(new String(new char[(int) p(1)])));

		// Relational, boolean and bitwise
		builtin("eq", () -> stack.push(stack.pop().equals(stack.pop())));
		builtin("ne", () -> stack.push(!stack.pop().equals(stack.pop())));
		builtin("gt", () -> stack.push(compare() < 0));
		builtin("lt", () -> stack.push(compare() > 0));
		builtin("ge", () -> stack.push(compare() <= 0));
		builtin("le", () -> stack.push(compare() >= 0));
		builtin("and", () -> stack.push(popBool() & popBool()));
		builtin("or",  () -> stack.push(popBool() | popBool()));
		builtin("xor", () -> stack.push(popBool() ^ popBool()));
		builtin("not", () -> stack.push(!popBool()));
		// bitshift
		vars.put("true", Boolean.TRUE);
		vars.put("false", Boolean.FALSE);
		vars.put("null", null);

		// Flow control
		builtin("exec", () -> execute(stack.pop()));
		builtin("stopped", () -> stack.push(Boolean.FALSE));
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
			final float max = p(1), inc = p(1);
			for (float i = p(1); i < max; i += inc) {
				stack.push(i);
				execute(code);
			}
		});
		builtin("forall", () -> {
			final Object code = stack.pop();
			final Object array = stack.pop();
			(array instanceof String ? ((String) array).chars().mapToObj(c -> c)
				: Arrays.stream((Object[]) array)).forEach(c -> {
					stack.push(c);
					execute(code);
				});
		});
		// exec

		// Type, attributes and conversion operators
		builtin("type", () -> stack.push(stack.pop() instanceof String ? "nametype" : null));
		// cvi
		builtin("cvx", NOOP);
		builtin("cvr", NOOP);
		builtin("cvlit", () -> stack.push(literal(stack.peek())));
		builtin("rcheck", () -> stack.push(stack.pop() != null));
		builtin("wcheck", () -> stack.push(stack.pop() != null));
		builtin("xcheck", () -> stack.push(stack.pop() != null));
		builtin("readonly", NOOP);
		builtin("executeonly", NOOP);

		// File operators
		builtin("==", () -> log.info(stack.pop().toString()));
		builtin("stack", () -> stack.stream().forEach(o -> log.info(o.toString())));

		// Miscellaneous
		vars.put("ps_level", 1f);
		builtin("currentglobal", () -> stack.push(Boolean.FALSE));
		builtin("bind", () -> stack.push(Arrays.stream((Object[]) stack.pop()).map(
			o -> vars.containsKey(o) ? vars.get(o) : o
		).toArray()));

		// Graphics State
		builtin("gsave", () -> g.save());
		builtin("grestore", () -> g.restore());
		builtin("grestoreall", () -> g.restore());
		builtin("setlinecap",    () -> g.setLineCap(popFlag()));
		builtin("setlinejoin",   () -> g.setLineJoin(popFlag()));
		builtin("setlinewidth",  () -> g.setStrokeWidth(p(1)));
		builtin("setmiterlimit", () -> g.setMiterLimit(p(1)));
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
			final Rectangle2D r = g.pathBounds();
			stack.push((float) r.getMinX());
			stack.push((float) r.getMinY());
			stack.push((float) r.getMaxX());
			stack.push((float) r.getMaxY());
		});

		builtin("currentcolortransfer", () -> stack.push(NOOP));
		builtin("currentblackgeneration", () -> stack.push(NOOP));
		builtin("currentundercolorremoval", () -> stack.push(NOOP));
		builtin("currentflat", () -> stack.push(0f));
		builtin("currentsmoothness", () -> stack.push(0f));
		builtin("setoverprint", () -> popBool());

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
		builtin("curveto", () -> g.lineTo(false, p(6), p(), p(), p(), p(), p()));
		builtin("closepath", () -> g.closePath());
		builtin("currentpoint", () -> {
			final Point2D point = g.getCurrentPoint();
			stack.push((float) point.getX());
			stack.push((float) point.getY());
		});

		// Painting
		builtin("stroke", () -> g.draw());
		builtin("fill", () -> g.fill(Path2D.WIND_NON_ZERO));
		builtin("eofill", () -> g.fill(Path2D.WIND_EVEN_ODD));
		builtin("clip", () -> g.clip());

		// Insideness-testing

		// Glyph and font
		builtin("definefont", () -> vars.put(pop2(), stack.peek()));
		builtin("findfont", () -> stack.push(((String) stack.pop()).replace('-', ' ')));
		builtin("scalefont", () -> g.setFontSize(p(1)));
		builtin("setfont", () -> g.setFont((String) stack.pop()));
		builtin("show", () -> {
			g.charpath((String) stack.pop());
			g.fill(Path2D.WIND_EVEN_ODD);
		});
		builtin("ashow", () -> {
			g.charpath((String) stack.pop());
			g.fill(Path2D.WIND_EVEN_ODD);
			p(2);
			p();
		});
		builtin("charpath", () -> {
			stack.pop(); // boolean stroke
			g.charpath((String) stack.pop());
		});

		// Unhandled but ignored
		builtin("defineresource", () -> {
			stack.pop();
			vars.put(pop2(), stack.peek());
		});
		builtin("findresource", () -> {
			pop2(); //category
			stack.push(vars.get(stack.pop())); // instance
		});
		builtin("currentscreen", () -> {
			stack.push(0f); //frequency
			stack.push(0f); //angle
			stack.push(0f); //halftone
		});
		builtin("setglobal", () -> stack.pop());
		builtin("save", () -> stack.push(null));
		builtin("restore", () -> stack.pop());
	}

	private int compare() {
		@SuppressWarnings("unchecked")
		final Comparable<Object> top = (Comparable<Object>) stack.pop();
		return top.compareTo(stack.pop());
	}

	@Override
	@SuppressWarnings("resource")
	public Graphics process(final ReadableByteChannel input) {
		final Scanner scanner = new Scanner(input, "ascii");
		g.getClip().intersect(new Area(new Rectangle2D.Float(0, 0, 612, 792)));

		// See PLRM 3.1: Syntax
		scanner.useDelimiter(String.format("(%1$s|(?=%2$s)|(?<=%2$s)|%%.*+)+",
					WHITESPACE, "[(){}<>\\[\\]/]"));
		while (scanner.hasNext()) {
			final Object obj = tokenize(scanner.next(), scanner);
			if (stack.contains(CURLY_MARK)) {
				// Deferred execution mode
				stack.push(obj);
			} else {
				execute(obj);
			}
		}
		return g;
	}

	private void execute(final Object object) {
		log.finest("Executing: " + object);
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
			final String hexString = scanner.findWithinHorizon(HEX_STRING, 0);
			scanner.next();
			return literal(DatatypeConverter.parseHexBinary(hexString.replaceAll(WHITESPACE, "")));
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
		if (!vars.containsKey(key)) {
		   throw new InputMismatchException("Unknown variable : " + key);
		}
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

	private boolean popBool() {
		return (Boolean) stack.pop();
	}

	/** Pops n items from the stack and returns them as an array. */
	private Object[] popTo(final Object mark) {
		final int n = stack.lastIndexOf(mark);
		assert n >= 0 : "No matching mark";
		final Object[] array = substack(stack.size() - 1 - n).toArray();
		stack.setSize(n);
		return array;
	}

	/** Returns a live view of the top n elements of the stack. */
	private List<Object> substack(final int n) {
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

	static final class PSDict extends HashMap<Object, Object> {
		@Override
		public boolean equals(final Object that) {
			return this == that;
		}

		@Override
		public int hashCode() {
			return 0;
		}
	}
}
