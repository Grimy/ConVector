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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Locale;

/**
 * Javadoc
 */
public class GLCBuilder {

	private enum OutputFormat {
		GCODE,
		SVG,
	}

	// public static final boolean win = System.getProperty("os.name").toLowerCase().equals("win");
	private OutputFormat format;
	private Module module;

	public GLCBuilder(Module module, OutputFormat format) {
		this.module = module;
		this.format = format;
	}

	public void parse(InputStream input, PrintStream output) {

		// Output the parsed instructions
		boolean svg = format == OutputFormat.SVG;
		if (svg) {
			output.print("<?xml version='1.0' encoding='UTF-8' standalone='no'?>\n");
			output.print("<!DOCTYPE svg PUBLIC '-//W3C//DTD SVG 1.0//EN' ");
			output.print("'http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd'>\n");
			output.print("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 300 300'>\n");
			output.print("<path transform='translate(0 300) scale(1 -1)' ");
			output.print("style='fill:none; stroke:#000000; stroke-width:2' d='");
		}

		Iterable<Instruction> instructions = module.process(input);
		for (Instruction cmd: instructions) {
			output.println(svg ? cmd.toSVG() : cmd.toGCode());
		}
	}

	private static void usage(int returnCode) {
		System.err.println("Usage: GLCBuilder [options] [input] [output]");
		System.exit(returnCode);
	}

	public static void main(String... args) {
		Locale.setDefault(Locale.US);

		// Parse arguments
		OutputFormat format = OutputFormat.GCODE;
		InputStream input = System.in;
		PrintStream output = System.out;
		String filetype = null;
		int i = 0;

		while (i < args.length && args[i].charAt(0) == '-') {
			switch (args[i++]) {
			case "-svg":
				format = OutputFormat.SVG;
				break;
			default:
				System.err.println("Unknown option: " + args[i]);
				usage(1);
			}
		}

		String filename = "";
		try {
			if (args.length > i) {
				input = new FileInputStream(args[i++]);
				filename = args[i - 1];
			}
			if (args.length > i) {
				output = new PrintStream(args[i++]);
			}
		} catch (FileNotFoundException e) {
			System.err.println("Cannot open file " + args[i - 1]);
			System.exit(2);
		}
		if (args.length > i) {
			System.err.println("Too many arguments.");
			usage(1);
		}

		Module module = pickModule(filename.substring(filename.lastIndexOf('.') + 1));
		GLCBuilder builder = new GLCBuilder(module, format);

		try {
			builder.parse(input, output);
		} catch (Exception e) {
			e.printStackTrace(); // XXX this is for debugging only
			System.err.println("Error while processing file " + filename);
			System.exit(3);
		}
	}

	private static Module pickModule(String extension) {
		// TODO: look at each module’s supported file type and return a list of possible modules
		switch (extension) {
		case "ngc":
		case "glc":
		case "gcode":
			return new GCodeImporter();
		case "ps":
			return new PSImporter();
		case "svg":
			return new SVGImporter();

		default:
			System.err.println("Unsupported file type : " + extension);
			System.exit(3);
			return null;
		}
	}
}
