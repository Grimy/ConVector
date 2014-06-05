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

package test;
import java.io.*;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import controller.GCodeBuilder;

public class GCodeCleanerTest {

	private static String[][] tests = {
		// Input                                            Expected output
		// delete whitespace
		{"    G01 \t  X10    Y  20   ",                     "G01 X10.0 Y20.0 Z0.0\n"},
		// delete empty lines
		{"G01 X10 Y20\n\n\n\nG01 X20 Y30",                  "G01 X10 Y20\nG01 X20 Y30"},
		// delete comments
		{";This is a comment",                              ""},
		{"G01 X10 Y20 (this is a comment)",                 "G01 X10.0 Y20.0 Z0.0\n"},
		// delete invalid functions
		{"G42 X10 Y20",                                     ""},
		// variables
		{"#1000 = 10\n#1001 = 20\nG01 X#1000 Y#1001",       "G01 X10.0 Y20.0 Z0.0\n"},
		// mathematical expressions
		{"G01 X[(10+20)*3/9] Y[5**2]",                      "G01 X10.0 Y25.0 Z0.0\n"},
		// mathematical expressions with variables
		{"#1000=10\n#1001=20\nG01 X[#1000*10] X[#1001*10]", "G01 X100 Y200 Z0.0\n"},
		// normalize function names
		{"G1 X10 Y20",                                      "G01 X10.0 Y20.0 Z0.0\n"},
		// repeated functions
		{"G01 X10 Y20\nX20 Y30",                            "G01 X10.0 Y20.0 Z0.0\nG01 X20.0 Y30.0 Z0.0\n"},
	};

	// TODO: test width, height, and cropping
	// negative values
	// {"G00 X-10 Y-10\nG01 X0 Y0",                        "G00 X0 Y0\nG01 X10 Y10"},

	public static void main(String... args) {
		new GCodeCleanerTest().testCleaner();
	}
	
	@Test
	public void testCleaner() {
		for (String[] test : tests) {
			String input = test[0];
			String expectedOutput = test[1];
			GCodeBuilder cleaner = new GCodeBuilder();
			BufferedReader reader = new BufferedReader(new StringReader(input));
			ByteArrayOutputStream writer = new ByteArrayOutputStream();
			cleaner.clean(reader, new PrintStream(writer));

			assertEquals(expectedOutput, writer.toString());
		}
	}
}
