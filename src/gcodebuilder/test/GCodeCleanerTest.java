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
import controller.GCodeCleaner;

public class GCodeCleanerTest {

	private static String[][] tests = {
		// Input                                            Expected output
		{"    G01    X10    Y  20   ",                      "G01 X10.0 Y20.0 Z0.0\n"},
		{";This is a comment",                              ""},
		{"G20 X10 Y20",                                     ""},
		{"G01 X[(10+20)*3/9] Y[5*2]",                       "G01 X10 Y25"},
		// wrong function name
		{"G42 X10 Y20",                                     "#WIDTH=0\n#HEIGHT=0"},
		// good and wrong function names
		{"G01 X10 Y20 G42 X10 Y20",                         "#WIDTH=0\n#HEIGHT=0\nG01 X10 Y20"},
		{"    G01    X10    Y  20   ",                      "#WIDTH=0\n#HEIGHT=0\nG01 X10 Y20"},
		{"G1 X10 Y20",                                      "#WIDTH=0\n#HEIGHT=0\nG01 X10 Y20"},
		{"G01 X10 Y20 (this is a comment)",                 "#WIDTH=0\n#HEIGHT=0\nG01 X10 Y20"},
		// 2 functions inline
		{"G01 X10 Y20 G01 X10 Y20",                         "#WIDTH=0\n#HEIGHT=0\nG01 X10 Y20\nG01 X10 Y20"},
		// multi-functions
		{"G01 X10 Y20\nX20 Y30",                            "#WIDTH=0\n#HEIGHT=0\nG01 X10 Y20\nG01 X20 Y30"},
		// delete empty lines
		{"G01 X10 Y20\n\n\n\nG01 X20 Y30",                  "#WIDTH=0\n#HEIGHT=0\nG01 X10 Y20\nG01 X20 Y30"},
		// mathematical expressions
		{"G01 X[(10+20)*3/9] Y[5**2]",                      "#WIDTH=0\n#HEIGHT=0\nG01 X10 Y25"},
		// variables
		{"#1000 = 10\n#1001 = 20\nG01 X#1000 Y#1001",       "#WIDTH=0\n#HEIGHT=0\nG01 X10 Y25"},
		// negative values
		{"G00 X-10 Y-10\nG01 X0 Y0",                        "#WIDTH=0\n#HEIGHT=0\nG00 X0 Y0\nG01 X10 Y10"},
		// variables with mathematical expressions
		{"#1000=10\n#1001=20\nG01 X[#1000*10] X[#1001*10]", "#WIDTH=0\n#HEIGHT=0\nG01 X100 Y200"},
	};

	public static void main(String... args) {
		new GCodeCleanerTest().testCleaner();
	}
	
	@Test
	public void testCleaner() {
		for (String[] test : tests) {
			String input = test[0];
			String expectedOutput = test[1];
			GCodeCleaner cleaner = new GCodeCleaner();
			BufferedReader reader = new BufferedReader(new StringReader(input));
			ByteArrayOutputStream writer = new ByteArrayOutputStream();
			cleaner.clean(reader, new PrintStream(writer));

			assertEquals(expectedOutput, writer.toString());
		}
	}
}
