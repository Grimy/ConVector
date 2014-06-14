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

package controller;

import java.io.*;
import model.Instruction;
import modules.*;

public class GLCBuilder {

	// public static final boolean win = System.getProperty("os.name").toLowerCase().equals("win");

	public static void main(String... args) {
		// TODO: method too long, refactor

		// Parse arguments
		boolean svg = false;
		int i = 0;

		for (i = 0; i < args.length && args[i].charAt(0) == '-'; i++) {
			switch (args[i]) {
			case "-svg":
				svg = true;
				break;
			default:
				System.err.println("Unknown option: " + args[i]);
				System.exit(1);
			}
		}

		if (args.length != i + 1) {
			System.err.println("Usage: GLCBuilder [filename]");
			System.exit(1);
		}
		String filename = args[i];

		// Open an input stream
		InputStream input = null;
		try {
			input = new FileInputStream(filename);
		} catch (FileNotFoundException e) {
			System.err.println("Cannot read file " + filename);
			System.exit(2);
		}

		// Pick a module and use it to parse the input stream
		Module module = pickModule(filename.substring(filename.lastIndexOf('.') + 1));
		Iterable<Instruction> instructions = null;
		try {
			instructions = module.process(input);
		} catch (Exception e) {
			System.err.println("Error while processing file " + filename);
			e.printStackTrace();
		}

		// Output the parsed instructions
		if (svg) {
			System.out.println("<?xml version='1.0' encoding='UTF-8' standalone='no'?>");
			System.out.println("<!DOCTYPE svg PUBLIC '-//W3C//DTD SVG 1.0//EN' "
					+ "'http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd'>");
			System.out.println("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 300 300'>");
			System.out.println("<path transform='translate(0 300) scale(1 -1)' stroke-width='3' d='");
		}

		for (Instruction cmd: instructions) {
			System.out.print(svg ? cmd.toSVG() : cmd.toGCode() + "\n");
		}

		if (svg) {
			System.out.println("'/></svg>");
		}
	}

	private static Module pickModule(String extension) {
		// TODO: look at each module’s supported file type and return a list of possible modules
		switch (extension) {
			case "gcode":
				return new GCodeImporter();
			case "ps":
			case "pdf":
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
