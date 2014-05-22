/*
 * This file is part of Drawall, a vertical tracer (aka drawbot) - see http://drawall.fr/
 * Drawall is free software and licenced under GNU GPL v3 : http://www.gnu.org/licenses/
 * Copyright (c) 2012-2014 Nathanaël Jourdane
 */

package controller;

import java.io.*;

public class Drawall {

	public static void main(String... args) {
		// TODO: parse args instead of hard-coding this
		BufferedReader in;
		PrintStream out;
		try {
			in = new BufferedReader(new FileReader("_.gcode"));
			out = new PrintStream(new File("_.glc"));
		} catch (Exception e) {
			// TODO: handle system exceptions properly
			System.err.println("Can't find file _.gcode");
			System.exit(1);
			return;
		}

		//GCodeCleaner.clean(in, out, "G00|G01");
	}
}
