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

package cc.drawall.ps;

import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

import javafx.scene.paint.Color;

import javax.xml.bind.DatatypeConverter;

import cc.drawall.Graphics;
import cc.drawall.Importer;

/** Importer used to parse PostScript. */
public class PSImporter implements Importer {
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
	private static final Runnable NOOP = () -> {/*NOOP*/};

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
	private final float[] stack = new float[512];
	private final List<Object> objects = new ArrayList<>();
	private int i = -1;
	private int itr = -1;

	private static final Object MARK = new Object();
	private static final Object CURLY_MARK = new Object();

	private final Graphics g = new Graphics();

	private final Map<Object, Void> literals = new IdentityHashMap<>();

	/** Main dictionary. */
	private final PSDict vars = new PSDict(); {
		// The categories and their order are from PLRM3 8.1: Operator Summary
		// Stack manipulation
		builtin("pop", () -> --i);
		builtin("exch", () -> {
			final float tmp = stack[i];
			stack[i] = stack[i - 1];
			stack[i - 1] = tmp;
		});
		builtin("dup", () -> stack[++i] = stack[i - 1]);
		builtin("copy", () -> {
			if (stack[i] == stack[i]) {
				int count = (int) stack[i];
				System.arraycopy(stack, i - count, stack, i, count);
				i += count - 1;
			} else {
				pop2();
			}
		});
		builtin("index", () -> stack[i] = stack[i - (int) stack[i]]);
		builtin("roll", () -> {
			int size = (int) stack[--i];
			int distance = ((int) stack[1 + i--] + size) % size;
			if (distance == 0)
				return;
			for (int cycleStart = i - size + 1, nMoved = 0; nMoved != size; cycleStart++) {
				float displaced = stack[cycleStart];
				int j = cycleStart;
				do {
					j += distance;
					if (j > i)
						j -= size;
					float tmp = stack[j];
					stack[j] = displaced;
					displaced = tmp;
					nMoved ++;
				} while (j != cycleStart);
			}
		});
		builtin("clear", () -> i = 0);
		builtin("cleartomark", () -> popTo(MARK));
		// count, mark, cleartomark, counttomark (not in PDF)

		// Math
		builtin("add", () -> stack[i - 1] += stack[i--]);
		builtin("div", () -> stack[i - 1] /= stack[i--]);
		// idiv, mod
		builtin("mul", () -> stack[i - 1] *= stack[i--]);
		builtin("sub", () -> stack[i - 1] -= stack[i--]);
		builtin("abs", () -> stack[i] = Math.abs(stack[i]));
		builtin("neg", () -> stack[i] = -stack[i]);
		// ceiling, floor, round, truncate, sqrt, atan, cos, sin, exp, ln, log
		// rand, srand, rrand

		// Array
		builtin("array", () -> stack[++i] = nanBox(literal(new Object[(int) p(1)])));
		vars.put("[", MARK);
		builtin("]", () -> stack[++i] = nanBox(literal(popTo(MARK))));
		// builtin("astore", () -> {
		// 	final Object[] array = (Object[]) stack.pop();
		// 	stack[++i] = substack(array.length).toArray(array));
		// });
		// builtin("length", () -> {
		// 	final Object o = stack.pop();
		// 	stack[++i] = o instanceof Object[] ? ((Object[]) o).length : 0f);
		// });

		// Dictionary
		vars.put("$error", vars);
		vars.put("errordict", vars);
		vars.put("userdict", vars);
		vars.put("statusdict", vars);
		vars.put("systemdict", vars);
		vars.put("currentdict", vars);
		vars.put("currentsystemparams", vars);
		builtin("countdictstack", () -> stack[++i] = 1f);
		// builtin("dictstack", () -> ((Object[]) stack.peek())[0] = vars);

		// builtin("dict", () -> stack[++i] = p(1) > 0 ? vars : null));
		builtin("begin", () -> --i);
		builtin("end", NOOP);
		builtin("load", () -> stack[i] = nanBox(getVar(nanUnbox(stack[i]))));
		builtin("def", () -> vars.put(nanUnbox(stack[--i]), nanUnbox(stack[1 + i--])));
		// builtin("get", () -> {
		// 	final Object o = pop2();
		// 	stack[++i] = o instanceof PSDict ? ((PSDict) o).get(stack.pop())
		// 			: o instanceof Object[] ? ((Object[]) o)[(int) p(1)]
		// 			: ((String) o).codePointAt((int) p(1)));
		// });
		// builtin("put", () -> {
		// 	final Object value = stack.pop();
		// 	final Object o = pop2();
		// 	if (o instanceof PSDict) {
		// 		((PSDict) o).put(stack.pop(), value);
		// 	} else {
		// 		((Object[]) o)[(int) p(1)] = value;
		// 	}
		// });
		// builtin("known", () -> stack[++i] = ((PSDict) pop2()).containsKey(stack.pop())));
		// builtin("where", () -> {
		// 	final boolean exists = vars.containsKey(stack.pop());
		// 	if (exists) {
		// 		stack[++i] = vars);
		// 	}
		// 	stack[++i] = exists);
		// });
		builtin("cleardictstack", NOOP);

		// String
		// builtin("string", () -> stack[++i] = new String(new char[(int) p(1)])));

		// Relational, boolean and bitwise
		builtin("eq", () -> stack[i - 1] = nanBox(stack[i - 1] == stack[i--]));
		builtin("ne", () -> stack[i - 1] = nanBox(stack[i - 1] != stack[i--]));
		builtin("le", () -> stack[i - 1] = nanBox(stack[i - 1] <= stack[i--]));
		builtin("ge", () -> stack[i - 1] = nanBox(stack[i - 1] >= stack[i--]));
		builtin("lt", () -> stack[i - 1] = nanBox(stack[i - 1] <  stack[i--]));
		builtin("gt", () -> stack[i - 1] = nanBox(stack[i - 1] >  stack[i--]));
		// builtin("and", () -> stack[--i] = popBool() & popBool()));
		// builtin("or",  () -> stack[--i] = popBool() | popBool()));
		// builtin("xor", () -> stack[--i] = popBool() ^ popBool()));
		// builtin("not", () -> stack[--i] = !popBool()));
		// bitshift
		vars.put("true", Boolean.TRUE);
		vars.put("false", Boolean.FALSE);
		vars.put("null", null);

		// Flow control
		// builtin("exec", () -> execute(stack.pop()));
		// builtin("stopped", () -> stack[++i] = Boolean.FALSE));
		// builtin("quit", NOOP);
		builtin("if", () -> {
			final float code = stack[i--];
			if ((Boolean) nanUnbox(stack[i--])) {
				execute(code);
			}
		});
		// builtin("ifelse", () -> {
		// 	final Object ifFalse = stack.pop();
		// 	final Object ifTrue = stack.pop();
		// 	execute((boolean) stack.pop() ? ifTrue : ifFalse);
		// });
		// builtin("repeat", () -> {
		// 	final Object code = stack.pop();
		// 	for (int i = (int) p(1); i > 0; i--) {
		// 		execute(code);
		// 	}
		// });
		// builtin("for", () -> {
		// 	final Object code = stack.pop();
		// 	final float max = p(1), inc = p(1);
		// 	for (float i = p(1); i < max; i += inc) {
		// 		stack[++i] = i);
		// 		execute(code);
		// 	}
		// });
		// builtin("forall", () -> {
		// 	final Object code = stack.pop();
		// 	final Object array = stack.pop();
		// 	(array instanceof String ? ((String) array).chars().mapToObj(c -> c)
		// 		: Arrays.stream((Object[]) array)).forEach(c -> {
		// 			stack[++i] = c);
		// 			execute(code);
		// 		});
		// });
		// exec

		// Type, attributes and conversion operators
		builtin("type", () -> stack[i] = nanBox(nanUnbox(stack[i]) instanceof String ? "nametype" : null));
		// cvi
		builtin("cvx", NOOP);
		builtin("cvr", NOOP);
		builtin("cvlit", () -> literal(nanUnbox(stack[i])));
		builtin("rcheck", () -> stack[++i] = nanBox(false));
		builtin("wcheck", () -> stack[++i] = nanBox(false));
		builtin("xcheck", () -> stack[++i] = nanBox(false));
		builtin("readonly", NOOP);
		builtin("executeonly", NOOP);

		// File operators
		builtin("==", () -> System.out.println(stack[i]));
		// builtin("stack", () -> Arrays.stream(stack).forEach(o -> System.out.println(o.toString())));

		// Miscellaneous
		vars.put("ps_level", 1f);
		builtin("currentglobal", () -> stack[++i] = (nanBox(Boolean.FALSE)));
		builtin("bind", () -> {
			float[] arr = (float[]) nanUnbox(stack[i]);
			for (int i = 0; i < arr.length; ++i) {
				arr[i] = vars.containsKey(arr[i]) ? nanBox(vars.get(arr[i])) : arr[i];
			}
		});

		// Graphics State
		builtin("gsave",         () -> g.save());
		builtin("grestore",      () -> g.resetPath().restore());
		builtin("grestoreall",   () -> g.resetPath().restore());
		builtin("setlinecap",    () -> g.setLineCap(Graphics.LineCap.values()[(int) p(1)]));
		builtin("setlinejoin",   () -> g.setLineJoin(Graphics.LineJoin.values()[(int) p(1)]));
		builtin("setlinewidth",  () -> g.setStrokeWidth(p(1)));
		builtin("setmiterlimit", () -> g.setMiterLimit(p(1)));
		builtin("setdash",       () -> g.setDashArray(popArray()).setDashOffset(p(1)));
		builtin("showpage", NOOP);
		builtin("setrgbcolor", () -> g.setColor(Graphics.Mode.BASE, Color.color(p(3), p(), p())));
		builtin("sethsbcolor", () -> g.setColor(Graphics.Mode.BASE, Color.hsb(p(3), p(), p())));
		builtin("setcmykcolor", () -> i -= 4);
		builtin("setgray", () -> g.setColor(Graphics.Mode.BASE, Color.gray(p(1))));
		builtin("clippath", () -> {
			g.resetPath();
			g.append(g.getClip());
		});
		builtin("pathbbox", () -> {
			final Rectangle2D r = g.pathBounds();
			stack[i++] = (float) r.getMinX();
			stack[i++] = (float) r.getMinY();
			stack[i++] = (float) r.getMaxX();
			stack[i++] = (float) r.getMaxY();
		});

		// builtin("currentcolortransfer", () -> stack[++i] = NOOP));
		// builtin("currentblackgeneration", () -> stack[++i] = NOOP));
		// builtin("currentundercolorremoval", () -> stack[++i] = NOOP));
		// builtin("currentflat", () -> stack[++i] = 0f));
		// builtin("currentsmoothness", () -> stack[++i] = 0f));
		// builtin("setoverprint", () -> popBool());

		// Coordinate systems
		// builtin("matrix", () -> stack[++i] = new float[]{1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f}));
		// currentmatrix
		builtin("setmatrix", () -> g.getTransform().setTransform(popMatrix()));
		builtin("concat", () -> g.getTransform().concatenate(popMatrix()));
		builtin("rotate", () -> g.getTransform().rotate(Math.toRadians(p(1))));
		builtin("scale", () -> g.getTransform().scale(p(2), p()));
		builtin("translate", () -> g.getTransform().translate(p(2), p()));

		// Path construction
		builtin("newpath", () -> g.resetPath());
		builtin("moveto", () -> g.setRelative(false).moveTo(p(2), p()));
		builtin("rmoveto", () -> g.setRelative(true).moveTo(p(2), p()));
		builtin("lineto", () -> g.setRelative(false).lineTo(p(2), p()));
		builtin("rlineto", () -> g.setRelative(true).lineTo(p(2), p()));
		builtin("curveto", () -> g.setRelative(false).lineTo(p(6), p(), p(), p(), p(), p()));
		builtin("rcurveto", () -> g.setRelative(true).lineTo(p(6), p(), p(), p(), p(), p()));
		builtin("closepath", () -> g.closePath());
		builtin("currentpoint", () -> {
			final Point2D point = g.getCurrentPoint();
			stack[++i] = (float) point.getX();
			stack[++i] = (float) point.getY();
		});

		// Painting
		builtin("stroke", () -> g.stroke().resetPath());
		builtin("fill", () -> g.setWindingRule(Path2D.WIND_NON_ZERO).fill().resetPath());
		builtin("eofill", () -> g.setWindingRule(Path2D.WIND_EVEN_ODD).fill().resetPath());
		builtin("clip", () -> g.clip(g.getPath()));

		// Insideness-testing

		// Glyph and font
		// builtin("definefont", () -> vars.put(pop2(), stack.peek()));
		// builtin("findfont", () -> stack[++i] = ((String) stack.pop()).replace('-', ' ')));
		// builtin("scalefont", () -> g.setFontSize(p(1)));
		// builtin("setfont", () -> g.setFont((String) stack.pop()));
		// builtin("show", () -> {
		// 	g.charpath((String) stack.pop());
		// 	g.fill();
		// });
		// builtin("ashow", () -> {
		// 	g.charpath((String) stack.pop());
		// 	g.fill();
		// 	p(2);
		// 	p();
		// });
		// builtin("charpath", () -> {
		// 	stack.pop(); // boolean stroke
		// 	g.charpath((String) stack.pop());
		// });

		// Unhandled but ignored
		// builtin("defineresource", () -> {
		// 	stack.pop();
		// 	vars.put(pop2(), stack.peek());
		// });
		// builtin("findresource", () -> {
		// 	pop2(); //category
		// 	stack[++i] = vars.get(stack.pop())); // instance
		// });
		builtin("currentscreen", () -> {
			stack[++i] = 0f; //frequency
			stack[++i] = 0f; //angle
			stack[++i] = 0f; //halftone
		});
		builtin("setglobal", () -> --i);
		builtin("save", () -> stack[++i] = 0f);
		builtin("restore", () -> --i);
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
			if (curlies > 0) {
				// Deferred execution mode
				stack[++i] = nanBox(obj);
			} else {
				execute(obj);
			}
		}
		return g;
	}

	private void execute(final float f) {
		if (f == f) {
			stack[++i] = f;
		} else {
			execute(nanUnbox(f));
		}
	}

	private void execute(final Object object) {
		if (literals.containsKey(object)) {
			stack[++i] = nanBox(object);
		} else if (object instanceof Runnable) {      // built-in operator
			((Runnable) object).run();
		} else if (object instanceof String) { // name object
			execute(getVar(object));
		} else if (object instanceof float[]) {   // procedure
			for (final float f: (float[]) object) {
				execute(f);
			}
		} else {
			stack[++i] = nanBox(object);
		}
	}

	int curlies;

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
			++curlies;
			return CURLY_MARK;
		case '}':
			final float[] proc = popTo(CURLY_MARK);
			return (Runnable) () -> stack[++i] = nanBox(proc);
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
		itr = i -= count;
		return stack[++itr];
	}

	private float p() {
		return stack[++itr];
	}

	private Object pop2() {
		final Object result = stack[i - 1];
		stack[i - 1] = stack[i--];
		return result;
	}

	/** Pops n items from the stack and returns them as an array. */
	private float[] popTo(final Object mark) {
		if (mark == CURLY_MARK) {
			--curlies;
		}
		int n = i + 1;
		while (nanUnbox(stack[--n]) != mark) {/*EMPTY*/}
		assert n >= 0 : "No matching mark";
		final float[] array = new float[i - n];
		System.arraycopy(stack, n + 1, array, 0, i - n);
		i = n - 1;
		return array;
	}

	private float[] popArray() {
		return (float[]) nanUnbox(stack[i--]);
	}

	private AffineTransform popMatrix() {
		final float[] matrix = popArray();
		assert matrix.length == 6;
		return new AffineTransform(matrix);
	}
	
	private float nanBox(Object object) {
		if (object instanceof Float) {
			return (Float) object;
		}
		objects.add(object);
		return Float.intBitsToFloat(~(objects.size() - 1));
	}

	private Object nanUnbox(float nan) {
		if (nan == nan) {
			return nan;
		}
		return objects.get(~Float.floatToRawIntBits(nan));
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
