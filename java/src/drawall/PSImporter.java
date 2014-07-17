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
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Pattern;

/** Plugin used to parse PostScript. */
public class PSImporter implements Plugin {
	/* PostScript® is a trademark of Adobe Systems Incorporated. */

	/** Conversion ratio for angles, from degrees to radians. */
	private static final double DEGREES_TO_RADIANS = 3.1415926535897932 / 180;

	/** A Runnable that does nothing, used for ignored instructions. */
	private static final Runnable NOOP = () -> {/* empty */};

	/* ==PATTERNS== */
	private static final Pattern NUMBER = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?");

	/* ==STACKS==
	 * The PostScript interpreter manages several stacks. See PSLR 3.4: Stacks.
	 * Here, some of those stacks are implemented by intrusive linked lists:
	 * each item links to the item underneath it on the stack.
	 * `null` represents an empty stack. */

	/** Operand stack. */
	// XXX: use a float[] instead for perf
	//      Objects would be stored in a separate heap, using signaling NaNs as indexes, eg:
	//      objects[~Float.floatToRawLongBits(x)] // First time I use the ~ operator for real!
	//      but then we have to do our own refcounting and garbage collecting… blargh… */
	private final Stack<Object> stack = new Stack<>();

	/** Dictionary stack. */
	// XXX: right now we only manage a single dictionary.
	private final Map<String, Runnable> vars = new HashMap<>();

	// TODO: execution stack

	/** Graphics state stack. */
	private GraphicsSave gsave = null;

	// TODO: clipping path stack

	public AffineTransform ctm = new AffineTransform();

	/** Stack of '[' marks’ positions.
	  * A corresponding ']' pops the operand stack until the top '[' mark. */
	private final Stack<Integer> marks = new Stack<>();


	/** Number of '}' until the end of the outermost code block. */
	private int curlyDepth = 0;

	/** The current code block, as a list of tokens. */
	private Vector<String> block = new Vector<>();


	/** The scanner used to parse the input. XXX: could be made local. */
	private Scanner scanner;

	private Graphics2D g;
	private Path2D path = new Path2D.Double();

	/** Fills the `vars` dictionnary with built-in operators. */
	public PSImporter() {

		// Fonts / colors
		vars.put("setrgbcolor", () -> g.setColor(new Color(
				(int) (255 * p(3)), (int) (255 * p()), (int) (255 * p()))));
		vars.put("setgray", () -> g.setColor(new Color((int) (0xFFFFFF * p(1)))));
		vars.put("findfont", () -> {
			stack.pop();
			while (!scanner.nextLine().equals("%%EndProlog")) {
				// TODO: handle font definitions
			}
		});

		// Paths
		vars.put("newpath", () -> path.reset());
		vars.put("moveto", () -> moveTo(p(2), p()));
		vars.put("lineto", () -> lineTo(p(2), p()));
		vars.put("rmoveto", () -> rMoveTo(p(2), p()));
		vars.put("rlineto", () -> rLineTo(p(2), p()));
		vars.put("curveto", () -> path.curveTo(p(6), p(), p(), p(), p(), p()));
		vars.put("closepath", () -> path.closePath());
		vars.put("stroke", () -> { g.draw(path); path.reset(); });
		vars.put("fill", () -> fill(Path2D.WIND_NON_ZERO));
		vars.put("eofill", () -> fill(Path2D.WIND_EVEN_ODD));
		vars.put("clip", () -> g.clip(path));

		// Computations
		vars.put("length", NOOP); // TODO
		vars.put("add", () -> stack.push(p(2) + p()));
		vars.put("sub", () -> stack.push(p(2) - p()));
		vars.put("mul", () -> stack.push(p(2) * p()));

		// Comparisons
		vars.put("eq", () -> stack.push(p(2) == p()));
		vars.put("ne", () -> stack.push(p(2) != p()));
		vars.put("not", () -> stack.push(!this.<Boolean>pop()));

		// Stack manipulation
		vars.put("dup", () -> stack.push(stack.peek()));
		vars.put("pop", () -> stack.pop());
		vars.put("exch", () -> {
			Object fst = stack.pop();
			Object snd = stack.pop();
			stack.push(fst);
			stack.push(snd);
		});
		vars.put("roll", () -> {
			int n = (int) p(2);
			int r = (int) p();
			Collections.rotate(stack.subList(stack.size() - n, stack.size()), r);
		});
		vars.put("[", () -> marks.push(stack.size()));
		vars.put("]", () -> {
			assert !marks.isEmpty() : "Unmatched ] mark";
			stack.push(popN(stack.size() - marks.pop()));
		});

		// Transformations matrices
		vars.put("matrix", () -> stack.push(new double[]{1.0, 0.0, 0.0, 1.0, 0.0, 0.0}));
		vars.put("concat", () -> ctm.concatenate(new AffineTransform(this.<double[]>pop())));
		vars.put("rotate", () -> ctm.rotate(p(1) * DEGREES_TO_RADIANS));
		// TODO: scale, translate

		// Graphics
		vars.put("gsave", () -> gsave = new GraphicsSave(g, ctm, path, gsave));
		vars.put("grestore", this::restore);
		vars.put("setlinecap", () -> ((MutableStroke) g.getStroke()).linecap = popFlag());
		vars.put("setlinejoin", () -> ((MutableStroke) g.getStroke()).linejoin = popFlag());
		vars.put("setlinewidth", () -> ((MutableStroke) g.getStroke()).linewidth = p(1));
		vars.put("setmiterlimit", () -> ((MutableStroke) g.getStroke()).miterLimit = p(1));
		vars.put("showpage", NOOP);

		// Variables
		vars.put("bind", NOOP);
		vars.put("load", () -> stack.push(getVar(this.<String>pop())));
		vars.put("def", () -> {
			Object value = stack.pop();
			// Convert non-code objects to code pushing them on the stack
			Runnable code = value instanceof Runnable ? (Runnable) value : () -> {
				stack.push(value);
			};
			String name = this.<String>pop();
			vars.put(name, code);
		});

		// Flow control
		vars.put("if", () -> {
			Runnable code = this.<Runnable>pop();
			if ((boolean) stack.pop()) {
				code.run();
			}
		});
		// TODO: ifelse, for, foreach

		// Unhandled
		vars.put("show", null);
		vars.put("ashow", null);
		vars.put("setcmykcolor", null);
	}

	@Override
	public void process(InputStream in, Graphics2D out) {
		scanner = new Scanner(in);
		g = out;
		g.setStroke(new MutableStroke());

		// Skip whitespace and comments, break around '[', ']', '{', '}' and before '/'
		scanner.useDelimiter("\\s*(?:\\s|(?=[{\\[\\]}/])|(?<=[{\\[\\]}])|%.*\\n)+");
		while (scanner.hasNext()) {
			accept(scanner.next());
		}
	}

	// private Object tokenToObject(String token) {
	// }

	// private void execute(Object object) {
		// if (curlyDepth == 0 && object instanceof Runnable) {
			// object.run();
		// } else {
			// stack.push(object);
		// }
	// }

	/** Process a single input token. */
	private void accept(String token) {
		char c = token.charAt(0);

		if (curlyDepth > 0) {
			curlyDepth += c == '{' ? 1 : c == '}' ? -1 : 0;
			if (curlyDepth > 0) {
				block.add(token);
			} else {
				final Vector<String> copy = block;
				Runnable code = () -> copy.forEach(this::accept);
				stack.push(code);
				block = new Vector<>();
			}
		} else if (c == '/') {
			stack.push(token.substring(1));
		} else if (c == '{') {
			curlyDepth++;
		} else if (c == '}') {
			throw new RuntimeException("Unmatched }");
		} else if (NUMBER.matcher(token).matches()) {
			stack.push(Double.parseDouble(token));
		} else {
			getVar(token).run();
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
		assert vars.containsKey(name) : "Undefined variable : " + name;
		return vars.get(name);
	}

	@SuppressWarnings("unchecked")
	private <T> T pop() {
		return (T) stack.pop();
	}

	// TODO: checking the range should be the responsibility of MutableStroke
	private int popFlag() {
		double val = this.<Double>pop();
		assert val >= 0 && val <= 2;
		return (int) val;
	}

	private class GraphicsSave {
		/** Current Transformation Matrix. */
		final Stroke stroke;
		final Color color;
		final Shape clip;
		final AffineTransform savedCtm;
		final Path2D savedPath;
		/** The previously saved graphical state. */
		final GraphicsSave prev;

		GraphicsSave(Graphics2D g, AffineTransform ctm, Path2D path, GraphicsSave prev) {
			stroke = ((MutableStroke) g.getStroke()).clone();
			color = g.getColor();
			clip = g.getClip();
			savedCtm = ctm;
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
		g.setStroke(gsave.stroke);
		g.setColor(gsave.color);
		g.setClip(gsave.clip);
		ctm = gsave.savedCtm;
		path = gsave.savedPath;
		gsave = gsave.prev;
	}

	// XXX move those to an helper class (RelativePath ?)
	public void moveTo(double x, double y) {
		path.moveTo(x(x, y), y());
	}

	public void lineTo(double x, double y) {
		path.lineTo(x(x, y), y());
	}

	public void rMoveTo(double x, double y) {
		Point2D p = path.getCurrentPoint();
		assert p != null;
		path.moveTo(p.getX() + x(x, y), p.getY() + y());
	}

	public void rLineTo(double x, double y) {
		Point2D p = path.getCurrentPoint();
		assert p != null;
		path.lineTo(p.getX() + x(x, y), p.getY() + y());
	}

	private Point2D transformed = new Point2D.Double();
	private double x(double x, double y) {
		ctm.transform(new Point2D.Double(x, y), transformed);
		return transformed.getX();
	}

	private double y() {
		return transformed.getY();
	}
}
