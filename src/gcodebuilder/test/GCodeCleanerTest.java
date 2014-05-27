/*
 * This file is part of Drawall, a vertical tracer (aka drawbot) - see http://drawall.cc/
 * Drawall is free software and licenced under GNU GPL v3 : http://www.gnu.org/licenses/
 * Copyright (c) 2012-2014 Nathanaël Jourdane
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
		String expected = "#WIDTH";
		assertEquals(cleanString(input), expected);
		
		// good and wrong function names
		input = "G01 X10 Y20 G42 X10 Y20";
		expected = "G01 X10 Y20";
		assertEquals(cleanString(input), expected);
		
		// spaces
		input = "    G01    X10    Y  20   ";
		expected = "G01 X10 Y20";
		assertEquals(cleanString(input), expected);

		// function name on 3 car.
		input = "G1 X10 Y20";
		expected = "G01 X10 Y20";
		assertEquals(cleanString(input), expected);

		// comments
		input = "G01 X10 Y20 (this is a comment)";
		expected = "G01 X10 Y20";
		assertEquals(cleanString(input), expected);

		// 2 functions inline
		input = "G01 X10 Y20 G01 X10 Y20";
		expected = "G01 X10 Y20\nG01 X10 Y20";
		assertEquals(cleanString(input), expected);

		// multi-functions
		input = "G01 X10 Y20\nX20 Y30";
		expected = "G01 X10 Y20\nG01 X20 Y30";
		assertEquals(cleanString(input), expected);

		// delete empty lines
		input = "G01 X10 Y20\n\n\n\nG01 X20 Y30";
		expected = "G01 X10 Y20\nG01 X20 Y30";
		assertEquals(cleanString(input), expected);
		
		// mathematical expressions
		input = "G01 X[(10+20)*3/9] Y[5**2]";
		expected = "G01 X10 Y25";
		assertEquals(cleanString(input), expected);

		// variables
		input = "#1000 = 10\n#1001 = 20\nG01 X#1000 Y#1001";
		expected = "G01 X10 Y25";
		assertEquals(cleanString(input), expected);

		// variables with mathematical expressions
		input = "#1000=10\n#1001=20\nG01 X[#1000*10] X[#1001*10]";
		expected = "G01 X100 Y200";
		assertEquals(cleanString(input), expected);

		// negative values
		input = "G00 X-10 Y-10\nG01 X0 Y0";
		expected = "G00 X0 Y0\nG01 X10 Y10";
		assertEquals(cleanString(input), expected);
	}
}
