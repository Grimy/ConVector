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
		if (args.length != 1) {
			System.err.println("Usage: GLCBuilder [filename]");
			System.exit(1);
		}

		String filename = args[0];
		InputStream input = null;
		try {
			input = new FileInputStream(filename);
		} catch (FileNotFoundException e) {
			System.err.println("Cannot read file " + filename);
			System.exit(2);
		}

		Module module = pickModule(filename.substring(filename.lastIndexOf('.') + 1));

		try {
			for (Instruction cmd: module.process(input)) {
				System.out.println(cmd.toGCode());
			}
		} catch (Exception e) {
			e.printStackTrace();
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
