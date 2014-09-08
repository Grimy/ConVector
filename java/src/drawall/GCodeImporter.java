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
import java.awt.geom.AffineTransform;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/* Plugin used to parse GCode. */
public class GCodeImporter implements Plugin {

	/* Convertion ratio. */
	private static final double INCHES_TO_MILLIMETERS = 25.4;

	/* Whether to use millimeters. Set by G20 / G21 */
	private static final AffineTransform ctm = new AffineTransform();

	/* Default mode for lines without an instruction. Set by G0 / G1. */
	private boolean write = false;

	/* Whether to compute coordinates relative to the current point. Set by G20 / G21 */
	private boolean relative = false;

	/** The scanner used to parse the input. */
	private Scanner scanner;

	/* Maps GCode variable names to their values. */
	private Map<Integer, Double> variables = new HashMap<>();

	private Graphics2D g;
	private RelativePath path = new RelativePath(ctm);

	@Override
	public void process(Reader input, Graphics2D output) {
		g = output;
		path.moveTo(false, 0, 0);
		g.setStroke(new BasicStroke(1));
		g.setColor(Color.BLACK);
		scanner = new Scanner(input);

		// Ignore whitespace and comments
		scanner.useDelimiter("(\\s|\\([^()]*\\)|^;.*\n)*+(?=[a-zA-Z=]|#[\\d\\s]+=|$)");

		// Main loop: iterate over tokens
		while (scanner.hasNext()) {
			String token = scanner.next().toUpperCase().replaceAll("\\s", "");
			double arg = parseDouble(token.substring(1));

			switch (token.charAt(0)) {
			case 'G':
				parseG((int) arg);
				break;
			case 'M':
				parseM((int) arg);
				break;
			case '#':
				variables.put((int) arg, readArg('='));
				break;

			// Ignored codes
			case 'F': // Set feedrate (TODO!)
			case 'T': // Select tool (TODO, not urgent)
			case 'S': // Set spindle speed
				// instructions.add(new Instruction(Instruction.Kind.PAUSE));
				break;

			case 'o': // Control flow
				throw new UnsupportedOperationException("Flow control isn’t implemented");

			default:
				throw new IllegalArgumentException("Invalid GCode: " + token);
			}
		}
	}

	/** Interprets a single G-code. */
	private void parseG(int code) {
		switch (code) {
		case 0:  // Linear motion without writing
		case 1:  // Linear motion
			write = code == 1;
			double x = scanner.hasNext("X.*") ? readArg('X') :
				relative ? 0 : Double.NaN; // path.getCurrentPoint().getX();
			double y = scanner.hasNext("Y.*") ? readArg('Y') :
				relative ? 0 : Double.NaN; // path.getCurrentPoint().getY();
			if (write) {
				path.lineTo(relative, x, y);
			} else {
				g.draw(path);
				path = new RelativePath(ctm);
				path.moveTo(relative, x, y);
			}
			break;

		case 5:  // Cubic spline
			path.curveTo(relative, readArg('I'), readArg('J'), readArg('P'), readArg('Q'),
					readArg('X'), readArg('Y'));
			break;

		case 20: // Switch to inches
			ctm.setToScale(INCHES_TO_MILLIMETERS, INCHES_TO_MILLIMETERS);
			break;

		case 21: // Switch to millimeters
			ctm.setToScale(1, 1);
			break;

		case 90: // Switch to absolute mode
		case 91: // Switch to relative mode
			relative = code == 91;
			break;

		case 17: // Select XY plane
		case 18: // Select XZ plane
		case 19: // Select YZ plane
			// XXX: die if plane != XY ?
			break;

		// Silently ignored codes
		case 4:  // Dwell
		case 33: // Spindle-synchronized motion
		case 40: // Cancel cutter radius compensation
		case 41: // Start cutter radius compensation left
		case 42: // Start cutter radius compensation right
		case 43: // Use tool length offset from tool table
		case 49: // Cancel tool length offset
		case 61: // Exact path mode
		case 64: // Continuous path mode
		case 73: // Peck drilling cycle
		case 76: // Multipass lathe threading cycle
		case 80: // Cancel motion mode
		case 81: // Drilling cycle without dwell
		case 82: // Drilling cycle with dwell
		case 83: // Chip-break drilling cycle
		case 85: // Boring cycle without dwell
		case 89: // Boring cycle with dwell
		case 93: // Inverse time feed rate
		case 94: // Units per minute feed rate
		case 95: // Units per revolution
		case 96: // CSS mode (Constant Surface Speed)
		case 97: // RPM mode
		case 98: // Retract to previous position
		case 99: // Retract to R
			break;

		// Unsupported codes (TODO)
		case 2:  // Helical motion, CW
		case 3:  // Helical motion, CCW
		case 7:  // Diameter mode
		case 8:  // Radius mode
		case 28: // Return to or set reference point 1
		case 30: // Return to or set reference point 2
		case 92: // Coordinate system offset
			throw new UnsupportedOperationException("Unimplemented GCode : G" + code);

		default:
			throw new UnsupportedOperationException("Invalid GCode : G" + code);
		}

		scanner.nextLine();
	}

	/** Interprets a single M-code. */
	private boolean parseM(int code) {
		switch (code) {
			case 2:  // End program
			case 30: // End program
				g.draw(path);
				break;

			case 0:  // Program pause
				break;

			// Silently ignored codes
			case 1:  // Optional pause
			case 3:  // Turn spindle CW
			case 4:  // Turn spindle CCW
			case 5:  // Stop spindle
			case 7:  // Turn mist on
			case 8:  // Turn flood on
			case 9:  // Turn all coolant off
			case 48: // Speed and Feed Override Control
			case 49: // Speed and Feed Override Control
			case 50: // Feed Override Control
			case 51: // Spindle Speed Override Control
			case 52: // Adaptive Feed Control
			case 53: // Feed Stop Control
			case 60: // Pallet change pause
				break;

			default:
				throw new UnsupportedOperationException("Invalid GCode : M" + code);
		}
		return false;
	}

	/**
	 * Reads a named argument from the file.
	 * Examples: given the input "X4.2", readArg('X') returns 4.2.
	 * @throws AssertionError when a mandatory argument isn’t found.
	 */
	private double readArg(char arg) {
		String token = scanner.next();
		assert token.charAt(0) == arg : "Required: " + arg + ", found: " + token;
		return parseDouble(token.substring(1));
	}

	/**
	 * Parses the specified string as a double.
	 * Handles GCode variables (#%d) and mathematical expressions.
	 */
	private double parseDouble(String string) {
		String expr = string;
		if (string.charAt(0) == '[') {
			expr = string.substring(1, string.length() - 1);
		}

		// Compute mathematical expressions
		double result = 0;
		for (String addend: expr.split("\\+")) {
			double product = 1;
			for (String factor: addend.split("\\*")) {
				product *= factor.charAt(0) == '#'
					? variables.get(Integer.parseInt(factor.substring(1)))
					: Double.parseDouble(factor);
			}
			result += product;
		}
		return result;
	}
}
