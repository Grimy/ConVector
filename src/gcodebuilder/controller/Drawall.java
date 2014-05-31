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

		new GCodeCleaner("G00|G01").clean(in, out);
	}
}
