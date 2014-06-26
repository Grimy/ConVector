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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;
import java.util.Vector;

/** Plugin used to parse PostScript. */
public class PSImporter implements Plugin {

	// A runnable that does nothing, used for ignored instructions.
	private static final Runnable NOOP = () -> {/* empty */};

	/** Saved graphical state. */
	private GraphicsSave gsave = null;

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
	private Path2D path = new Path2D.Double();

	/** Fills the `vars` dictionnary with built-in operators. */
	public PSImporter() {

		// Fonts / colors
		vars.put("setrgbcolor", () -> {
			int blue = popColor(), green = popColor(), red = popColor();
			g.setColor(new Color(red, green, blue));
		});
		vars.put("setgray", () -> g.setColor(new Color(65793 * popColor())));
		vars.put("findfont", () -> {
			stack.pop();
			while (!scanner.nextLine().equals("%%EndProlog")) {
				// TODO: handle font definitions
			}
		});

		// Paths
		vars.put("newpath", () -> path.reset());
		vars.put("moveto", () -> path.moveTo(p(2), p()));
		vars.put("lineto", () -> path.lineTo(p(2), p()));
		vars.put("curveto", () -> path.curveTo(p(6), p(), p(), p(), p(), p()));
		vars.put("closepath", () -> path.closePath());
		vars.put("stroke", () -> { g.draw(path); path.reset(); });
		vars.put("fill", () -> fill(Path2D.WIND_NON_ZERO));
		vars.put("eofill", () -> fill(Path2D.WIND_EVEN_ODD));
		vars.put("clip", () -> g.clip(path));

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
		vars.put("matrix", () -> stack.push(new double[]{1.0, 0.0, 0.0, 1.0, 0.0, 0.0}));
		vars.put("concat", () -> g.transform(new AffineTransform(this.<double[]>pop())));
		// TODO: scale, translate, rotate

		// Graphics
		vars.put("gsave", () -> gsave = new GraphicsSave(g, path, gsave));
		vars.put("grestore", this::restore);
		vars.put("setlinecap", () -> ((MutableStroke) g.getStroke()).linecap = popFlag());
		vars.put("setlinejoin", () -> ((MutableStroke) g.getStroke()).linejoin = popFlag());
		vars.put("setlinewidth", () -> ((MutableStroke) g.getStroke()).linewidth = this.<Double>pop());
		vars.put("setmiterlimit", () -> ((MutableStroke) g.getStroke()).miterLimit = this.<Double>pop());

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
		g.setStroke(new MutableStroke());
		g.translate(0, 300);
		g.scale(1, -1);

		// Skip whitespace and comments, break around '[', ']', '{', '}' and before '/'
		scanner.useDelimiter("\\s*(?:\\s|(?=[{\\[\\]}/])|(?<=[{\\[\\]}])|%.*\\n)+");
		while (scanner.hasNext()) {
			accept(scanner.next());
		}
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

	private Iterator<Object> itr;

	private double p(int n) {
		itr = new Vector<>(stack.subList(stack.size() - n, stack.size())).iterator();
		stack.setSize(stack.size() - n);
		return (Double) itr.next();
	}

	private double p() {
		return (Double) itr.next();
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
		stack.setSize(stack.size() - n);
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

	private int popFlag() {
		double val = this.<Double>pop();
		assert val >= 0 && val <= 2;
		return (int) val;
	}

	private class GraphicsSave {
		/** Current Transformation Matrix. */
		final AffineTransform ctm;
		final Stroke stroke;
		final Color color;
		final Shape clip;
		final Path2D savedPath;
		/** The previously saved graphical state. */
		final GraphicsSave prev;

		GraphicsSave(Graphics2D g, Path2D path, GraphicsSave prev) {
			ctm = g.getTransform();
			stroke = ((MutableStroke) g.getStroke()).clone();
			color = g.getColor();
			clip = g.getClip();
			savedPath = (Path2D) path.clone();
			this.prev = prev;
		}
	}

	private class MutableStroke implements Stroke, Cloneable {
		/** Current linecap. 0 = buttcap, 1 = round cap, 2 = projecting cap. */
		int linecap = 0;

		/** Current line join. 0 = Miter join, 1 = round join, 2 = Bevel join. */
		int linejoin = 0;

		/** Below this angle, a Bevel join is used instead of a Miter join. */
		double miterLimit = 10;

		/** Line width for stroked paths. */
		double linewidth = 1;

		// float dash_phase
		// float[] dash

		MutableStroke() {}

		@Override
		public Shape createStrokedShape(Shape s) {
			return new BasicStroke((float) linewidth, linecap, linejoin, (float) miterLimit).createStrokedShape(s);
			//, dash, dash_phase);
		}

		@Override
		public MutableStroke clone() {
			try {
				return (MutableStroke) super.clone();
			} catch (CloneNotSupportedException e) {
				throw new AssertionError();
			}
		}
	}

	private void fill(int windingRule) {
		path.setWindingRule(windingRule);
		g.fill(path);
		path.reset();
	}

	void restore() {
		g.setTransform(gsave.ctm);
		g.setStroke(gsave.stroke);
		g.setColor(gsave.color);
		g.setClip(gsave.clip);
		path = gsave.savedPath;
		gsave = gsave.prev;
	}
}
