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

package cc.drawall.gcode;

import java.io.InputStream;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

import cc.drawall.Importer;
import cc.drawall.WriterGraphics2D;

/* Importer used to parse GCode. */
public class GCodeImporter implements Importer {
	private static final Logger log = Logger.getLogger(GCodeImporter.class.getName());

	/* Convertion ratio. */
	private static final double INCHES_TO_MM = 25.4;

	/* Whether to compute coordinates relative to the current point. Set by G20 / G21 */
	private boolean relative = false;

	/** The scanner used to parse the input. */
	private Scanner scanner;

	private WriterGraphics2D g;

	/* Maps GCode variable names to their values. */
	private final Map<Integer, Float> variables = new HashMap<>();

	private final Map<Integer, Runnable> gcodes = new HashMap<>(); {
		gcodes.put(0, () -> g.lineTo(relative, readPos('X'), readPos('Y')));
		gcodes.put(1, () -> {
			g.draw();
			g.moveTo(relative, readPos('X'), readPos('Y'));
		});
		gcodes.put(5, () -> g.curveTo(relative, readArg('I'), readArg('J'),
					readArg('P'), readArg('Q'), readArg('X'), readArg('Y')));
		gcodes.put(20, () -> g.getTransform().setToScale(INCHES_TO_MM, INCHES_TO_MM));
		gcodes.put(21, () -> g.getTransform().setToScale(1, 1));
		gcodes.put(90, () -> relative = false);
		gcodes.put(91, () -> relative = true);

		final Runnable unsupported = () -> log.severe("Unsupported operation");
		gcodes.put(2,  unsupported);
		gcodes.put(3,  unsupported);
		gcodes.put(7,  unsupported);
		gcodes.put(8,  unsupported);
		gcodes.put(18, unsupported);
		gcodes.put(19, unsupported);
		gcodes.put(28, unsupported);
		gcodes.put(30, unsupported);
		gcodes.put(92, unsupported);
		// 2:  Helical motion, CW
		// 3:  Helical motion, CCW
		// 7:  Diameter mode
		// 8:  Radius mode
		// 18: Select XZ plane
		// 19: Select YZ plane
		// 28: Return to or set reference point 1
		// 30: Return to or set reference point 2
		// 92: Coordinate system offset
	}

	private final Map<Integer, Runnable> mcodes = new HashMap<>(); {
		mcodes.put(2,  () -> g.draw());
		mcodes.put(30, () -> g.draw());
	}

	@Override
	public void process(final InputStream input, final WriterGraphics2D output) {
		scanner = new Scanner(input, "ascii");
		g = output;
		g.moveTo(false, 0, 0);

		// Ignore whitespace and comments
		scanner.useDelimiter("(\\s|\\([^()]*\\)|^;.*\n)*+(?=[a-zA-Z=]|#[\\d\\s]+=|$)");

		// Main loop: iterate over tokens
		while (scanner.hasNext()) {
			final String token = scanner.next().toUpperCase(Locale.US).replaceAll("\\s", "");
			final float arg = parseFloat(token.substring(1));

			switch (token.charAt(0)) {
			case 'G':
				gcodes.getOrDefault((int) arg, () -> log.finest("G" + arg)).run();
				scanner.nextLine();
				break;
			case 'M':
				mcodes.getOrDefault((int) arg, () -> log.finest("M" + arg)).run();
				break;
			case '#':
				variables.put((int) arg, readArg('='));
				break;

			// Ignored codes
			case 'F': // Set feedrate
			case 'T': // Select tool
			case 'S': // Set spindle speed
				break;

			case 'o': // Control flow
			default:
				throw new InputMismatchException("Invalid GCode: " + token);
			}
		}
	}

	private float readPos(final char axis) {
		return scanner.hasNext(axis + ".*") ? readArg(axis) : relative ? 0 : Float.NaN;
	}

	/**
	 * Reads a named argument from the file.
	 * Examples: given the input "X4.2", readArg('X') returns 4.2.
	 * @throws AssertionError when a mandatory argument isn’t found.
	 */
	private float readArg(final char arg) {
		final String token = scanner.next();
		assert token.charAt(0) == arg : "Required: " + arg + ", found: " + token;
		return parseFloat(token.substring(1));
	}

	/**
	 * Parses the specified string as a float.
	 * Handles GCode variables (#%d) and mathematical expressions.
	 */
	private float parseFloat(final String string) {
		String expr = string.charAt(0) == '[' ? string.substring(1, string.length() - 1)
		                                      : string;

		// Compute mathematical expressions
		float result = 0;
		for (final String addend: expr.split("\\+")) {
			float product = 1;
			for (final String factor: addend.split("\\*")) {
				product *= factor.charAt(0) == '#'
					? variables.get(Integer.valueOf(factor.substring(1)))
					: Float.parseFloat(factor);
			}
			result += product;
		}
		return result;
	}
}
