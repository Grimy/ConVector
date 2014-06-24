/*
 * This file is part of DraWall.
 * DraWall is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * DraWall is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with DraWall. If not, see <http://www.gnu.org/licenses/>.
 * Copyright (c) 2012-2014 Nathanaël Jourdane.
 */

package drawall;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;
import java.util.Vector;

/** Plugin used to parse PostScript. */
public class PSImporter implements Plugin {

	// A runnable that does nothing, used for ignored instructions.
	private static final Runnable NOOP = () -> {/* empty */};

	/** Current graphical state. */
	private PSGraphics graphics = new PSGraphics();

	/** Main PostScript stack (aka operand stack). */
	private final Stack<Object> stack = new Stack<>();

	/** Stack of '[' marks’ positions.
	  * A corresponding ']' pops the operand stack until the top '[' mark. */
	private final Stack<Integer> marks = new Stack<>();

	/** Maps variable names to their values. */
	private final Map<String, Runnable> vars = new HashMap<>();

	/** The scanner used to parse the input. XXX: could be made local. */
	private Scanner scanner;

	private Graphics2D g;

	/** Fills the `vars` dictionnary with built-in operators. */
	public PSImporter() {

		// Fonts / colors
		vars.put("setrgbcolor", () -> {
			int blue = popColor(), green = popColor(), red = popColor();
			g.setColor(new Color(red, green, blue));
		});
		vars.put("setgray", () -> {
			int gray = popColor();
			g.setColor(new Color(gray, gray, gray));
		});
		vars.put("findfont", () -> {
			stack.pop();
			while (!scanner.nextLine().equals("%%EndProlog")) {
				// TODO: handle font definitions
			}
		});

		// Paths
		vars.put("newpath", () -> graphics.path.reset());
		vars.put("moveto", () -> {
			double[] p = popPoints(1);
			graphics.path.moveTo(p[0], p[1]);
		});
		vars.put("lineto", () -> {
			double[] p = popPoints(1);
			graphics.path.lineTo(p[0], p[1]);
		});
		vars.put("curveto", () -> {
			double[] p = popPoints(3);
			graphics.path.curveTo(p[0], p[1], p[2], p[3], p[4], p[5]);
		});
		vars.put("closepath", () -> graphics.path.closePath());
		vars.put("stroke", () -> {
			g.draw(graphics.path);
			graphics.path.reset();
		});
		vars.put("fill", () -> {
			graphics.path.setWindingRule(Path2D.WIND_NON_ZERO);
			g.fill(graphics.path);
			graphics.path.reset();
		});
		vars.put("eofill", () -> {
			graphics.path.setWindingRule(Path2D.WIND_EVEN_ODD);
			g.fill(graphics.path);
			graphics.path.reset();
		});
		vars.put("clip", NOOP); // TODO (no clear)

		// Computations
		vars.put("length", NOOP);
		// TODO: mul, add, sub

		// Stack manipulation
		vars.put("dup", () -> stack.push(stack.peek()));
		vars.put("pop", () -> stack.pop());
		vars.put("exch", () -> {
			Object fst = stack.pop();
			Object snd = stack.pop();
			stack.push(fst);
			stack.push(snd);
		});
		// TODO: roll

		// Transformations matrices
		vars.put("matrix", () -> stack.push(PSGraphics.idMatrix()));
		vars.put("concat", () -> {
			double[] a = this.<double[]>pop();
			double[] b = graphics.ctm.clone();
			double[] c = graphics.ctm;
			c[0] = a[0] * b[0] + a[1] * b[2];
			c[1] = a[0] * b[1] + a[1] * b[3];
			c[2] = a[2] * b[0] + a[3] * b[2];
			c[3] = a[2] * b[1] + a[3] * b[3];
			c[4] = a[4] * b[0] + a[5] * b[2] + b[4];
			c[5] = a[4] * b[1] + a[5] * b[3] + b[5];
		});
		// TODO: scale, translate, rotate

		// Graphics
		vars.put("gsave", () -> graphics = graphics.dup());
		vars.put("grestore", () -> graphics = graphics.prev);
		vars.put("setlinecap", () -> graphics.linecap = popFlag());
		vars.put("setlinejoin", () -> graphics.linejoin = popFlag());
		vars.put("setlinewidth", () -> graphics.linewidth = this.<Double>pop());
		vars.put("setmiterlimit", () -> graphics.miterLimit = this.<Double>pop());

		// Variables
		vars.put("bind", NOOP);
		vars.put("load", () -> stack.push(getVar(this.<String>pop())));
		vars.put("def", () -> {
			Object value = stack.pop();
			// Convert non-code objects to code pushing them on the stack
			Runnable code = value instanceof Runnable ? (Runnable) value : () -> {
				assert value != null;
				stack.push(value);
			};
			String name = this.<String>pop();
			vars.put(name, code);
		});

		// Flow control
		vars.put("if", () -> {
			Runnable code = this.<Runnable>pop();
			boolean condition = (boolean) stack.pop();
			if (condition) {
				code.run();
			}
		});
		// TODO: ifelse, for, foreach
	}

	@Override
	public void process(InputStream in, Graphics2D out) {
		scanner = new Scanner(in);
		g = out;
		// Skip whitespace and comments, break around '[', ']', '{', '}' and before '/'
		scanner.useDelimiter("\\s*(?:\\s|(?=[{\\[\\]}/])|(?<=[{\\[\\]}])|%.*\\n)+");
		while (scanner.hasNext()) {
			accept(scanner.next());
		}
		// result.add(new Instruction(Instruction.Kind.END));
	}

	/** Process a single input token. */
	private void accept(String token) {
		char c = token.charAt(0);
		if (c == '/') {
			stack.push(token.substring(1));
		} else if (c == '{') {
			Vector<String> vector = new Vector<>();
			int depth = 1;
			for (;;) {
				String s = scanner.next();
				depth += s.equals("{") ? 1 : s.equals("}") ? -1 : 0;
				if (depth == 0) {
					break;
				}
				vector.add(s);
			}
			Runnable code = () -> vector.forEach(this::accept);
			stack.push(code);
		} else if (Character.isAlphabetic(c)) {
			getVar(token).run();
		} else if (c == '.' || c == '-' || Character.isDigit(c)) {
			stack.push(Double.parseDouble(token));
		} else if (c == '[') {
			marks.push(stack.size());
		} else if (c == ']') {
			assert !marks.isEmpty() : "Unmatched ] mark";
			stack.push(popN(stack.size() - marks.pop()));
		} else {
			throw new RuntimeException("Unknown token : " + token);
		}
	}

	/** Pops 2*n numbers from the stack, treating them as a list of (X, Y) coordinates.
	  * Applies the current transformation matrix and returns the result. */
	private double[] popPoints(int n) {
		double[] points = (double[]) popN(2 * n);
		double[] a = graphics.ctm;
		for (int i = 0; i < 2 * n; i += 2) {
			double tmp = points[i] * a[0] + points[i + 1] * a[2] + a[4];
			points[i + 1] = points[i] * a[1] + points[i + 1] * a[3] + a[5];
			points[i] = tmp;
		}
		return points;
	}

	/** Pops n items from the stack and returns them as an array.
	  * If possible, returns a double[] (perf). Otherwise, falls back to Object[]. */
	private Object popN(int n) {
		assert n >= 0 && n <= stack.size();

		if (stack.peek() instanceof Double) {
			double[] array = new double[n];
			for (int i = n - 1; i >= 0; --i) {
				array[i] = this.<Double>pop();
			}
			return array;
		}
		List<Object> sublist = stack.subList(stack.size() - n, stack.size());
		Object[] array = sublist.toArray();
		sublist.clear();
		return array;
	}

	/** Returns the value of the specified variable. */
	private Runnable getVar(String name) {
		Runnable value = vars.get(name);
		// assert value != null;
		return value;
	}

	/** Pops a specific type from the stack. XXX: generify this */
	private int popColor() {
		double val = this.<Double>pop();
		assert val >= 0 && val <= 1;
		return (int) (val * 255);
	}

	@SuppressWarnings("unchecked")
	private <T> T pop() {
		return (T) stack.pop();
	}

	private byte popFlag() {
		double val = this.<Double>pop();
		assert val >= 0 && val <= 2;
		return (byte) val;
	}
}
