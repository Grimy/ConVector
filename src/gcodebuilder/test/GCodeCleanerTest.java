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

	private String cleanString(String str_test) {
		BufferedReader to_clean = new BufferedReader(new StringReader(str_test));
		PrintStream cleaned = new PrintStream(new ByteArrayOutputStream());
		GCodeCleaner cleaner = new GCodeCleaner();
		cleaner.clean(to_clean, cleaned);

		return cleaned.toString();
	}
	
	@Test
	public void testCleaner() {

		// wrong function name
		String input = "G42 X10 Y20";
		String expected = "#WIDTH=0\n#HEIGHT=0";
		assertEquals(cleanString(input), expected);
		
		// good and wrong function names
		input = "G01 X10 Y20 G42 X10 Y20";
		expected = "#WIDTH=0\n#HEIGHT=0\nG01 X10 Y20";
		assertEquals(cleanString(input), expected);

		input = "G42 X10 Y20 G01 X10 Y20";
		expected = "#WIDTH=0\n#HEIGHT=0\nG01 X10 Y20";
		assertEquals(cleanString(input), expected);
		
		// spaces
		input = "    G01    X10    Y  20   ";
		expected = "#WIDTH=0\n#HEIGHT=0\nG01 X10 Y20";
		assertEquals(cleanString(input), expected);

		// function name on 3 car.
		input = "G1 X10 Y20";
		expected = "#WIDTH=0\n#HEIGHT=0\nG01 X10 Y20";
		assertEquals(cleanString(input), expected);

		// comments
		input = "G01 X10 Y20 (this is a comment)";
		expected = "#WIDTH=0\n#HEIGHT=0\nG01 X10 Y20";
		assertEquals(cleanString(input), expected);

		// 2 functions inline
		input = "G01 X10 Y20 G01 X10 Y20";
		expected = "#WIDTH=0\n#HEIGHT=0\nG01 X10 Y20\nG01 X10 Y20";
		assertEquals(cleanString(input), expected);

		// multi-functions
		input = "G01 X10 Y20\nX20 Y30";
		expected = "#WIDTH=0\n#HEIGHT=0\nG01 X10 Y20\nG01 X20 Y30";
		assertEquals(cleanString(input), expected);

		// delete empty lines
		input = "G01 X10 Y20\n\n\n\nG01 X20 Y30";
		expected = "#WIDTH=0\n#HEIGHT=0\nG01 X10 Y20\nG01 X20 Y30";
		assertEquals(cleanString(input), expected);
		
		// mathematical expressions
		input = "G01 X[(10+20)*3/9] Y[5**2]";
		expected = "#WIDTH=0\n#HEIGHT=0\nG01 X10 Y25";
		assertEquals(cleanString(input), expected);

		// variables
		input = "#1000 = 10\n#1001 = 20\nG01 X#1000 Y#1001";
		expected = "#WIDTH=0\n#HEIGHT=0\nG01 X10 Y25";
		assertEquals(cleanString(input), expected);

		// variables with mathematical expressions
		input = "#1000=10\n#1001=20\nG01 X[#1000*10] X[#1001*10]";
		expected = "#WIDTH=0\n#HEIGHT=0\nG01 X100 Y200";
		assertEquals(cleanString(input), expected);

		// negative values
		input = "G00 X-10 Y-10\nG01 X0 Y0";
		expected = "#WIDTH=0\n#HEIGHT=0\nG00 X0 Y0\nG01 X10 Y10";
		assertEquals(cleanString(input), expected);
	}
}
