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

public class Drawall {

	public static void main(String... args) {
		// TODO: parse args instead of hard-coding this
		Module module = new GCodeImporter();
		for (Instruction cmd: module.process("_.gcode")) {
			System.out.println(cmd.toGCode());
		}
			// in = new BufferedReader(new FileReader("_.gcode"));
			// out = new PrintStream(new File("_.glc"));
	}
}
