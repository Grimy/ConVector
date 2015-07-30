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

package cc.drawall.gcode;

import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

import cc.drawall.Canvas;
import cc.drawall.Importer;
import cc.drawall.Output;

/** Importer used to parse GCode. */
public class GCodeImporter implements Importer {
	/* Conversion ratio. */
	private static final double INCHES_TO_MM = 25.4;

	private static final Pattern TOKEN = Pattern.compile("[GMFTSO#]"
		+ "[-+]?\\d*\\.?\\d+(E[-+]?\\d+)?", Pattern.CASE_INSENSITIVE);

	/* Maps GCode variable names to their values. */
	private final Map<Integer, Float> variables = new HashMap<>();

	/* Scanner on the input data */
	private Scanner scanner;

	private Canvas g;

	private static final Map<Integer, Runnable> gcodes = new HashMap<>(); {
		gcodes.put(0,  () -> {g.stroke().resetPath(); g.moveTo(readPos('X'), readPos('Y'));});
		gcodes.put(10, () -> g.lineTo(readPos('X'), readPos('Y')));
		gcodes.put(50, () -> g.lineTo(readArg('I'), readArg('J'),
			readArg('P'), readArg('Q'), readArg('X'), readArg('Y')));
		gcodes.put(51, () -> g.lineTo(readArg('I'), readArg('J'),
			readArg('X'), readArg('Y')));
		gcodes.put(20, () -> g.getTransform().setToScale(INCHES_TO_MM, INCHES_TO_MM));
		gcodes.put(21, () -> g.getTransform().setToScale(1, 1));
		gcodes.put(90, () -> g.setRelative(false));
		gcodes.put(91, () -> g.setRelative(true));
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

	@Override
	public void process(final ReadableByteChannel input, final Output output) {
		g = new Canvas(output);
		scanner = new Scanner(input, "ascii");

		// Ignore whitespace and comments
		scanner.useDelimiter("(\\s|\\([^()]*\\)|;.*\n)*+(?=[a-zA-Z=]|#[\\d\\s]+=|$)");
		scanner.skip("(?:; (\\d+)x(\\d+)\n)?");

		g.setSize(65535, 65535);
		g.getTransform().scale(.01f, -.01f);
		g.getTransform().translate(0, -65535);
		g.setStrokeWidth(100f);
		g.moveTo(0, 0);

		// Main loop: iterate over tokens
		while (scanner.hasNext()) {
			final String token = scanner.next(TOKEN).toUpperCase(Locale.US).replaceAll("\\s", "");
			final float arg = parseFloat(token.substring(1));

			switch (token.charAt(0)) {
			case 'G':
				gcodes.getOrDefault((int) (arg * 10), () -> {/*NOOP*/}).run();
				scanner.nextLine();
				break;
			case '#':
				variables.put((int) arg, readArg('='));
				break;
			case 'M':
			case 'F': // Set feedrate
			case 'T': // Select tool
			case 'S': // Set spindle speed
				break;
			case 'O': // Control flow
			default:
				throw new InputMismatchException("Invalid GCode: " + token);
			}
		}
		g.stroke();
	}

	private float readPos(final char axis) {
		return scanner.hasNext(axis + ".*") ? readArg(axis) : Float.NaN;
	}

	/** Reads a named argument from the file.
	  * Examples: given the input "X4.2", readArg('X') returns 4.2.
	  * @throws AssertionError when a mandatory argument isn’t found. */
	private float readArg(final char arg) {
		final String token = scanner.next();
		assert token.charAt(0) == arg : "Required: " + arg + ", found: " + token;
		return parseFloat(token.substring(1));
	}

	/** Parses the specified string as a float.
	  * Handles GCode variables (#%d) and mathematical expressions. */
	private float parseFloat(final String string) {
		final String expr = string.charAt(0) == '['
			? string.substring(1, string.length() - 1) : string;

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
