package modules;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;
import java.util.regex.*;
import model.Instruction;

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
		scanner.useDelimiter("\\s*(\\([^()]\\))?\\s+(^;.*\n)*");

		Collection<Instruction> result = new ArrayList<>();

		while (scanner.hasNext()) {
			parse(scanner.next());
		}

		return result;
	}

	private void parse(String token) {
		boolean on = token.charAt(token.length() - 1) == '1';

		switch (token) {
		case "G0":
		case "G00":
		case "G1":
		case "G01":
			write = on;
			setPos('X', getArg('X'));
			setPos('Y', getArg('Y'));
			setPos('Z', getArg('Z'));
			break;

		case "G20":
		case "G21":
			metric = on;
			break;

		case "G90":
		case "G91":
			relative = on;
			break;

		default:
			throw new UnsupportedOperationException("Unimplemented or invalid GCode : " + token);
		}
	}

	private double getArg(char arg) {
		Pattern pattern = Pattern.compile(arg + "[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?");
		return scanner.hasNext(pattern) ? Double.parseDouble(scanner.next(pattern).substring(1))
		                                : Double.NaN;
	}
}
