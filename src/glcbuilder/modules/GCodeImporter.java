package modules;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.*;
import model.*;

public class GCodeImporter implements Module {

	private static final double INCHES_TO_MILLIMETERS = 25.4;
	private static final Pattern TOKEN = Pattern.compile("\\d+(\\.\\d+)?|#\\d+|\\[.*?\\]")

	private boolean write = false;
	private boolean metric = false;
	private boolean relative = false;

	/** The currently selected axis. This is used by arc motions (G2 and G3). */
	private char axis = 'Z';

	private double[] pos = { 0.0, 0.0, 0.0 };
	private double[] minPos = { Double.POSITIVE_INFINITY,
		Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY };
	private double[] maxPos = { Double.NEGATIVE_INFINITY,
		Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY };

	private Scanner scanner;
	private Map<Integer, Double> variables = new HashMap<>();
	private Collection<Instruction> instructions = new ArrayList<>();

	@Override
	public String getParamTypes() {
		return "[]";
	}

	@Override
	public String getSupportedFormats() {
		return ".ngc,.gcode";
	}

	@Override
	public String getDescription() {
		return "Import a GCode file.";
	}

	@Override
	public Collection<Instruction> process(String inputFilePath) {
		try {
			scanner = new Scanner(new File(inputFilePath));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		// Ignore whitespace and comments
		scanner.useDelimiter("(\\s|\\([^()]*\\)|^;.*\n)+");

		while (scanner.hasNext()) {
			String token = scanner.next();
			switch (token.charAt(0)) {
			case 'G':
				parseG(Integer.parseInt(token.substring(1)));
				break;
			case 'M':
				parseM(Integer.parseInt(token.substring(1)));
				break;
			case '#':
				parseVar(Integer.parseInt(token.substring(1)));
				break;

			// Ignored codes
			case 'F': // Set feedrate (TODO!)
			case 'T': // Select tool (TODO, not urgent)
			case 'S': // Set spindle speed
				break;

			case 'o': // Control flow
				throw new UnsupportedOperationException("Unimplemented GCode : G" + token);

			default:
				throw new IllegalArgumentException("Invalid GCode: " + token);
			}
		}

		return instructions;
	}

	private double getPos(char param) {
		return pos[param - 'X'];
	}

	private void setPos(char param, double value) {
		if (Double.isNaN(value)) {
			return;
		}
		if (!metric) {
			value *= INCHES_TO_MILLIMETERS;
		}
		if (relative) {
			value += getPos(param);
		}

		int i = param - 'X';
		pos[i] = value;
		minPos[i] = value < minPos[i] ? value : minPos[i];
		maxPos[i] = value > maxPos[i] ? value : maxPos[i];
	}

	private void parseG(int code) {
		switch (code) {
		case 0:  // Linear motion without writing
		case 1:  // Linear motion
			write = code == 1;
			setPos('X', readArg('X', true));
			setPos('Y', readArg('Y', true));
			setPos('Z', readArg('Z', true));
			instructions.add(new DrawLine(getPos('X'), getPos('Y'), getPos('Z'), write));
			break;

		case 4:  // Dwell
			instructions.add(new Dwell(readArg('P', false)));

		case 20: // Switch to inches
		case 21: // Switch to millimeters
			metric = code == 21;
			break;

		case 90: // Switch to absolute mode
		case 91: // Switch to relative mode
			relative = code == 91;
			break;

		case 17: // Select XY plane
		case 18: // Select XZ plane
		case 19: // Select YZ plane
			axis = (char) ('Z' - (code - 17));
			break;

		// Silently ignored codes
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
		case 98: // Retract to previous postion
		case 99: // Retract to R
			scanner.nextLine();
			break;

		// Unsupported codes (TODO)
		case 2:  // Helical motion, CW
		case 3:  // Helical motion, CCW
		case 5:  // Cubic spline
		case 7:  // Diameter mode
		case 8:  // Radius mode
		case 28: // Return to or set reference point 1
		case 30: // Return to or set reference point 2
		case 92: // Coordinate system offset
			throw new UnsupportedOperationException("Unimplemented GCode : G" + code);

		default:
			throw new UnsupportedOperationException("Invalid GCode : G" + code);
		}
	}

	private boolean parseM(int code) {
		switch (code) {
			case 2:  // End program
			case 30: // End program
				instructions.add(new EndProgram());

			case 0:  // Program pause
				instructions.add(new Pause());

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

	private void parseVar(int var) {
		String token = scanner.next();
		if (!token.equals("=")) {
			throw new IllegalArgumentException("Invalid variable declaration: " + token);
		}
		variables.put(var, scanner.nextDouble());
	}

	/**
	 * Reads a named argument from the file.
	 * Throws an exception when a mandatory argument isn’t found.
	 * Returns NaN when an optional argument isn’t found.
	 * Examples: given the input "X4.2", readArg('X') returns 4.2.
	 */
	private double readArg(char arg, boolean optional) {
		if (optional && !scanner.hasNext(arg + ".*")) {
			return Double.NaN;
		}
		String token = scanner.next();
		if (!optional && token.charAt(0) != arg) {
			throw new IllegalArgumentException("");
		}

		if (token.charAt(1) != '[') {
			return parseRealValue(token.substring(1));
		}

		// Compute mathematical expressions
		double result = 0;
		for (String addend: token.substring(2, token.length() - 2).split("\\+")) {
			double product = 1;
			for (String factor: addend.split("\\*")) {
				product *= parseRealValue(factor);
			}
			result += product;
		}
		return result;
	}

	/**
	 * Parses the specified string as a double. Handles GCode variables (#%d).
	 */
	private double parseRealValue(String string) {
		return string.charAt(0) == '#' ? variables.get(Integer.parseInt(string.substring(1)))
		                               : Double.parseDouble(string);
	}
}
