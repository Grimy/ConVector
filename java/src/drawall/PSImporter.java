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

package drawall;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.bind.DatatypeConverter;

/** Plugin used to parse PostScript. */
public class PSImporter implements Plugin {
	/** PostScript® is a trademark of Adobe Systems Incorporated. */

	/** The correspondance between PostScript type and Java types is as follow:
	  * Operator    Runnable
	  * Real        Double
	  * Integer     Double
	  * Boolean     Boolean
	  * Array       Object[]
	  * Dictionary  Map<Object, Object>
	  * Name        String
	  * String      byte[] (PostScript strings are mutable)
	  **/

	/** Conversion ratio for angles, from degrees to radians. */
	private static final double DEGREES_TO_RADIANS = 3.1415926535897932 / 180;

	/** A Runnable that does nothing, used for ignored instructions. */
	private static final Runnable NOOP = () -> {/* empty */};

	/* ==PATTERNS== */
	private static final Pattern NUMBER = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?");
	private static final Pattern STRING = Pattern.compile("(?:\\\\.|[^()])*");
	private static final Pattern HEX_STRING = Pattern.compile("[0-9a-fA-F \r\n]*");

	/* ==STACKS==
	 * The PostScript interpreter manages several stacks. See PLRM 3.4: Stacks.
	 * Here, some of those stacks are implemented by intrusive linked lists:
	 * each item links to the item underneath it on the stack.
	 * `null` represents an empty stack. */

	/** Operand stack. */
	private final Stack<Object> stack = new Stack<>();

	/** Dictionary stack. */
	// XXX: right now we only manage a single dictionary.
	private final Map<Object, Object> vars = new HashMap<>();

	/** Stroke settings. */
	private MutableStroke stroke = new MutableStroke();

	/** Current Transformation Matrix. */
	private final AffineTransform ctm = new AffineTransform();

	/** Current path. */
	private RelativePath path = new RelativePath(ctm);

	private static final Object MARK = new Object();
	private static final Object CURLY_MARK = new Object();

	/** The scanner used to parse the input. XXX: could be made local. */
	private Scanner scanner;

	private Graphics2D g;

	/** Fills the `vars` dictionary with built-in operators. */
	public PSImporter() {

		// The categories and their order are from PLRM3 8.1: Operator Summary

		// Stack manipulation
		builtin("pop", () -> stack.pop());
		builtin("exch", () -> Collections.rotate(substack(2), 1));
		builtin("dup", () -> stack.push(stack.peek()));
		// builtin("copy", null); // TODO
		builtin("index", () -> stack.push(stack.get(stack.size() - (int) p(1)))); // untested; watch for off-by-one errors
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
		// rand, srand, rrand (not in PDF)

		// Array
		// array
		vars.put("[", MARK);
		builtin("]", () -> stack.push(popTo(MARK)));
		builtin("astore", () -> stack.push(substack(((Object[]) stack.pop()).length).toArray())); // untested
		// builtin("length", () -> stack.push((double) ((Map<?, ?>) stack.pop()).size()));
	   builtin("length", () -> { stack.pop(); stack.push(0.0); });

		// Dictionary
		vars.put("systemdict", vars);
		vars.put("currentdict", vars);
		builtin("dict", () -> {p(1); stack.push(vars);});
		builtin("begin", () -> stack.pop()); // TODO
		builtin("end", NOOP); // TODO
		builtin("load", () -> stack.push(getVar(stack.pop())));
		builtin("def", () -> vars.put(pop2(), stack.pop()));
		builtin("get", () -> {
			Object o = pop2();
			stack.push(o instanceof Map ? ((Map<?, ?>) o).get(stack.pop()) : ((Object[]) o)[(int) p(1)]);
		});
		builtin("known", () -> stack.push(((Map<?, ?>) pop2()).containsKey(stack.pop())));
		builtin("where", () -> stack.push(false)); // TODO
		builtin("cleardictstack", NOOP); // TODO

		// String
		// TODO

		// Relational, boolean and bitwise
		builtin("eq", () -> stack.push(p(2) == p())); // use .equals instead?
		builtin("ne", () -> stack.push(p(2) != p()));
		builtin("gt", () -> stack.push(compare() > 0));
		builtin("lt", () -> stack.push(compare() < 0));
		// ge, lt, le, and, or, xor
		builtin("not", () -> stack.push(!(Boolean) stack.pop()));
		// bitshift
		vars.put("true", true);
		vars.put("false", false);

		// Flow control
		builtin("exec", () -> execute(stack.pop()));
		builtin("quit", NOOP);
		builtin("if", () -> {
			Object code = stack.pop();
			if ((boolean) stack.pop()) {
				execute(code);
			}
		});
		builtin("ifelse", () -> {
			Object ifFalse = stack.pop();
			Object ifTrue = stack.pop();
			execute((boolean) stack.pop() ? ifTrue : ifFalse);
		});
		builtin("repeat", () -> {
			Object code = stack.pop();
			for (int i = (int) p(1); i > 0; i--) {
				execute(code);
			}
		});
		builtin("for", () -> {
			Object code = stack.pop();
			for (double i = p(3), inc = p(), max = p(); i < max; i += inc) {
				stack.push(i);
				execute(code);
			}
		});
		builtin("forall", () -> {
			Object code = stack.pop();
			if (stack.peek() instanceof Font) return;
			for (Object o: (Object[]) stack.pop()) {
				stack.push(o);
				execute(code);
			}
		});
		// exec, for (not in PDF)

		// Type, attributes and conversion operators
		// cvi, cvr
		builtin("readonly", NOOP);

		// File operators
		builtin("==", () -> System.out.println(stack.pop()));
		builtin("stack", () -> stack.stream().forEach(System.out::println));

		// Miscellaneous
		builtin("bind", () -> stack.push(((List<?>) stack.pop()).stream().map(
			o -> vars.containsKey(o) ? vars.get(o) : o
		).collect(Collectors.toList())));
			// List<Object> proc = (List<Object>) stack.peek();
			// for (int i = 0; i < proc.size(); i++) {
				// if (vars.containsKey(proc.get(i))) {
					// proc.set(i, getVar(proc.get(i)));
				// }
			// }
		// });

		// Graphics State
		builtin("gsave", () -> prev = new PSImporter().copy(this));
		builtin("grestore", () -> copy(prev));
		builtin("setlinecap",    () -> stroke.set("cap", popFlag()));
		builtin("setlinejoin",   () -> stroke.set("join", popFlag()));
		builtin("setlinewidth",  () -> stroke.set("width", (float) p(1)));
		builtin("setmiterlimit", () -> stroke.set("miterlimit", (float) p(1)));
		builtin("setdash",       () -> {
			stroke.set("dash", Arrays.stream((Object[]) stack.pop()).mapToDouble(o -> (Double) o).toArray());
			stroke.set("phase", p(1));
		});
		// "dash", "phase"
		builtin("showpage", NOOP);
		// TODO new Color(ColorSpace.getInstance(TYPE_CMYK), float[], 1.0);
		builtin("setrgbcolor", () -> g.setColor(new Color((float) p(3), (float) p(), (float) p())));
		builtin("sethsbcolor", () -> g.setColor(Color.getHSBColor((float) p(3), (float) p(), (float) p())));
		builtin("setcmykcolor", () -> substack(4).clear());
		builtin("setgray", () -> g.setColor(new Color((int) (0xFFFFFF * p(1)))));
		builtin("clippath", () -> addToPath(g.getClip())); // XXX should clearpath first
		builtin("pathbbox", () -> {
			Rectangle2D r = path.getBounds2D();
			stack.push(r.getMinX());
			stack.push(r.getMinY());
			stack.push(r.getMaxX());
			stack.push(r.getMaxY());
		});

		// Coordinate systems
		builtin("matrix", () -> stack.push(new Double[]{1.0, 0.0, 0.0, 1.0, 0.0, 0.0}));
		builtin("currentmatrix", () -> ctm.getMatrix((double[]) stack.peek())); // untested; double/Double ?
		builtin("setmatrix", () -> ctm.setTransform(popMatrix()));
		builtin("concat", () -> ctm.concatenate(popMatrix()));
		builtin("rotate", () -> ctm.rotate(p(1) * DEGREES_TO_RADIANS));
		builtin("scale", () -> ctm.scale(p(2), p()));
		builtin("translate", () -> ctm.translate(p(2), p()));

		// Path construction
		builtin("newpath", () -> path.reset());
		builtin("moveto", () -> path.moveTo(false, p(2), p()));
		builtin("rmoveto", () -> path.moveTo(true, p(2), p()));
		builtin("lineto", () -> path.lineTo(false, p(2), p()));
		builtin("rlineto", () -> path.lineTo(true, p(2), p()));
		builtin("curveto", () -> path.curveTo(false, p(6), p(), p(), p(), p(), p()));
		builtin("closepath", () -> path.closePath());
		builtin("currentpoint", () -> {
			stack.push(path.getCurrentPoint().getX());
			stack.push(path.getCurrentPoint().getY());
		});

		// Painting
		builtin("stroke", () -> {
			// XXX: this is an ugly hack to make stroke-width scale with the transform
			try {
				g.fill(ctm.createTransformedShape(stroke.createStrokedShape(
							ctm.createInverse().createTransformedShape(path))));
			} catch (NoninvertibleTransformException e) {
				/* do nothing */
			}
			path.reset();
		});
		builtin("fill", () -> fill(Path2D.WIND_NON_ZERO));
		builtin("eofill", () -> fill(Path2D.WIND_EVEN_ODD));
		builtin("clip", () -> g.clip(path));

		// Insideness-testing
		// TODO?

		// Glyph and font
		builtin("definefont", () -> vars.put(pop2(), stack.peek()));
		builtin("findfont", () -> stack.push(new Font((String) stack.pop(), 0, 1)));
		builtin("scalefont", () -> {
			double scale = p(1);
			Font font = (Font) stack.pop();
			AffineTransform transform = font.getTransform();
			transform.scale(scale, scale);
			stack.push(font.deriveFont(transform));
		});
		builtin("setfont", () -> g.setFont((Font) stack.pop()));
		builtin("show", () -> show((String) stack.pop(), 0, 0));
		builtin("ashow", () -> show((String) stack.pop(), p(2), p()));
		builtin("charpath", () -> {
			path.reset();
			stack.pop(); // boolean stroke
			String str = (String) stack.pop();
			addToPath(g.getFont().createGlyphVector(g.getFontRenderContext(), str).getOutline());
		});

		// vars.put("FontDirectory", new HashMap<>());
		builtin("save", () -> stack.push(null)); // TODO
		builtin("restore", () -> stack.pop()); // TODO
	}

	private void show(String str, double x, double y) {
		// System.out.println(str + ", " + x + ", " + y);
		assert g.getFont() != null;
		assert path.getCurrentPoint() != null;
		// Map<TextAttribute, Object> attributes = new HashMap<>();
		// attributes.put(TextAttribute.TRACKING, x);
		// g.setFont(g.getFont().deriveFont(attributes));
		g.drawString(str, (float) path.getCurrentPoint().getX(), (float) path.getCurrentPoint().getY());
	}

	private int compare() {
		@SuppressWarnings("unchecked")
		Comparable<Object> top = (Comparable<Object>) stack.pop();
		return -top.compareTo(stack.pop());
	}

	private void addToPath(Shape s) {
		// XXX code here is duplicated from Utils.optimize
		Utils.eachSegment(s, null, (coords, type) -> {
			switch (type) {
				case PathIterator.SEG_MOVETO:
					path.moveTo(false, coords[0], coords[1]);
					break;
				case PathIterator.SEG_LINETO:
					path.lineTo(false, coords[0], coords[1]);
					break;
				case PathIterator.SEG_QUADTO:
					path.quadTo(false, coords[0], coords[1], coords[2], coords[3]);
					break;
				case PathIterator.SEG_CUBICTO:
					path.curveTo(false, coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
					break;
				case PathIterator.SEG_CLOSE:
					path.closePath();
					break;
				default:
					assert false;
			}
		});
	}

	@Override
	public void process(Reader in, Graphics2D out) {
		scanner = new Scanner(in);
		g = out;
		g.setClip(new Rectangle2D.Double(0, 0, 65535, 65535));

		// See PLRM 3.1: Syntax
		String delimiters = "[(){}<>\\[\\]/]";
		scanner.useDelimiter("([\000\t\r\n\f ]|(?=" + delimiters + ")|(?<=" + delimiters + ")|%.*+)+");
		while (scanner.hasNext()) {
			Object obj = tokenize(scanner.next());
			if (stack.contains(CURLY_MARK)) {
				// Deferred execution mode
				stack.push(obj);
			} else {
				execute(obj);
			}
		}
	}

	private void execute(Object object) {
		System.out.println(object);
		if (literals.containsKey(object)) {
			stack.push(object);
		} else if (object instanceof Runnable) {      // built-in operator
			((Runnable) object).run();
		} else if (object instanceof String) { // name object
			execute(getVar(object));
		} else if (object instanceof List) {   // procedure
			((List<?>) object).stream().forEach(this::execute);
		} else {
			stack.push(object);
		}
	}

	/** Process a single input token. */
	private Object tokenize(String token) {
		switch (token.charAt(0)) {
		case '(':
			String string = scanner.findWithinHorizon(STRING, 0);
			scanner.next(); // find(")");
			return literal(string); //.getBytes();
		case '<':
			String hexString = scanner.findWithinHorizon(HEX_STRING, 0).replaceAll("[\r\n ]", "");
			scanner.next(); // find(">");
			return literal(DatatypeConverter.parseHexBinary(hexString));
		case '{':
			return CURLY_MARK;
		case '}':
			List<?> proc = Arrays.asList(popTo(CURLY_MARK));
			return (Runnable) () -> stack.push(proc);
		case '/':
			return literal(scanner.next());
		default:
			return NUMBER.matcher(token).matches() ? Double.parseDouble(token) : token;
		}
	}

	private Map<Object, Void> literals = new IdentityHashMap<>();
	private Object literal(Object obj) {
		literals.put(obj, null);
		return obj;
	}

	///////////////////////
	// Dict manipulation //
	///////////////////////

	private void builtin(String key, Runnable operation) {
		vars.put(key, operation);
	}

	private Object getVar(Object key) {
		assert vars.containsKey(key) : "Unknown variable : " + key;
		return vars.get(key);
	}

	/////////////////////////
	// Stack manipualation //
	/////////////////////////

	private Iterator<Object> itr;
	private double p(int n) {
		itr = new Vector<>(substack(n)).iterator();
		stack.setSize(stack.size() - n);
		return p();
	}

	private double p() {
		return (Double) itr.next();
	}

	private Object pop2() {
		Object fst = stack.pop();
		Object snd = stack.pop();
		stack.push(fst);
		return snd;
	}

	/** Pops n items from the stack and returns them as an array. */
	private Object[] popTo(Object mark) {
		int n = stack.lastIndexOf(mark);
		assert n >= 0 : "No matching " + (mark == MARK ? '[' : '{');
		Object[] array = substack(stack.size() - 1 - n).toArray();
		stack.setSize(n);
		return array;
	}

	/** Returns a live view of the top n elements of the stack. */
	private List<Object> substack(int n) {
		return stack.subList(stack.size() - n, stack.size());
	}

	private int popFlag() {
		double val = p(1);
		assert val >= 0 && val < 3;
		return (int) val;
	}

	private AffineTransform popMatrix() {
		Object[] matrix = (Object[]) stack.pop();
		assert matrix.length == 6 : "Transformation matrix should have 6 elements";
		return new AffineTransform(Arrays.stream(matrix).mapToDouble(o -> (Double) o).toArray());
	}


	/** Saved graphical context. */
	private PSImporter prev = null;

	PSImporter copy(PSImporter that) {
		this.stroke = that.stroke.clone();
		this.ctm.setTransform(that.ctm);
		this.path = new RelativePath(that.path);
		this.g = this.g == null ? (Graphics2D) that.g.create() : this.g;
		this.g.setColor(that.g.getColor()); // XXX is this necessary after cloning?
		this.g.setClip(that.g.getClip());
		this.g.setFont(that.g.getFont());
		this.prev = that.prev;
		return this;
	}

	/** Allows dynamically modifying stroking parameters. See BasicStroke. */
	private static class MutableStroke extends BasicStroke implements Cloneable {
		MutableStroke() {
			super(1, 0, 0, 10);
		}

		@Override
		public MutableStroke clone() {
			try {
				return (MutableStroke) super.clone();
			} catch (CloneNotSupportedException e) {
				throw new AssertionError("Clone failed");
			}
		}

		void set(String fieldName, Object value) {
			try {
				Field field = BasicStroke.class.getDeclaredField(fieldName);
				field.setAccessible(true);
				field.set(this, value);
			} catch (Exception e) {
				// XXX handle this better
				System.err.println("Reflection failed for " + fieldName + " = " + value);
				throw new RuntimeException(e);
			}
		}
	}

	private void fill(int windingRule) {
		path.setWindingRule(windingRule);
		g.fill(path);
		path.reset();
	}
}
