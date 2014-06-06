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
	private boolean write = false;
	private boolean metric = false;
	private boolean relative = false;

	private double[] pos = { 0.0, 0.0, 0.0 };
	private double[] minPos = { Double.POSITIVE_INFINITY,
		Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY };
	private double[] maxPos = { Double.NEGATIVE_INFINITY,
		Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY };

	private Scanner scanner;
	private Map<Integer, Double> variables = new HashMap<>();
	private Collection<Instruction> instructions = new ArrayList<>();

	public double getPos(char param) {
		return pos[param - 'X'];
	}

	public void setPos(char param, double value) {
		if (Double.isNaN(value)) {
			return;
		}
		int i = param - 'X';
		pos[i] = value;
		minPos[i] = value < minPos[i] ? value : minPos[i];
		maxPos[i] = value > maxPos[i] ? value : maxPos[i];
	}

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

			case 'F': // set feedrate
			case 'S': // set spindle speed
			case 'T': // select tool
				// ignored
				break;
			default:
				throw new UnsupportedOperationException("Unimplemented or invalid GCode: " + token);
			}
		}

		return instructions;
	}

	private void parseG(int code) {
		boolean on = code % 10 == 1;

		switch (code) {
		case 0:
		case 1:
			write = on;
			setPos('X', getArg('X'));
			setPos('Y', getArg('Y'));
			setPos('Z', getArg('Z'));
			instructions.add(new DrawLine(getPos('X'), getPos('Y'), getPos('Z'), write));
			break;

		case 4:
			instructions.add(new Dwell(getArg('P')));

		case 20:
		case 21:
			metric = on;
			break;

		case 90:
		case 91:
			relative = on;
			break;

		// Plane selection; ignored at the moment
		case 17:
		case 18:
		case 19:
			break;

		// Exact / continuous mode
		case 64:
			getArg('P');
		case 61:
			break;

		default:
			throw new UnsupportedOperationException("Unimplemented or invalid GCode : G" + code);
		}
	}

	private boolean parseM(int code) {
		switch (code) {

			case 3:
			case 4:
				getArg('S');
				break;

			case 2:  // End program
			case 5:  // Stop spindle
			case 7:  // Turn mist on
			case 8:  // Turn flood on
			case 9:  // Turn all coolant off
			case 30: // End program
				break;

			default:
				throw new UnsupportedOperationException("Unimplemented or invalid GCode : M" + code);
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

	private double getArg(char arg) {
		if (!scanner.hasNext(arg + ".*")) {
			return Double.NaN;
		}
		String token = scanner.next().substring(1);
		if (token.charAt(0) != '[') {
			return getValue(token);
		}
		double result = 1;
		for (String operand: token.substring(1, token.length() - 2).split("\\*")) {
			result *= getValue(operand);
		}
		return result;
	}

	private double getValue(String string) {
		return string.charAt(0) == '#' ? variables.get(Integer.parseInt(string.substring(1)))
		                               : Double.parseDouble(string);
	}
}
