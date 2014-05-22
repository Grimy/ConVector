/*
 * This file is part of Drawall, a vertical tracer (aka drawbot) - see http://drawall.fr/
 * Drawall is free software and licenced under GNU GPL v3 : http://www.gnu.org/licenses/
 * Copyright (c) 2012-2014 Nathanaël Jourdane
 */

package test;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import drawall.GCodeCleaner;

public class GCodeCleanerTest {

    @Test
    public void testWrongFunction() {
        String input = "G20 X10 Y20";
        // TODO call GCodeCleaner()
        String expected = "";

        assertEquals(input, expected);
    }

    @Test
    public void testMathExpression() {
        String input = "G01 X[(10+20)*3/9] Y[5**2]";
        // TODO call GCodeCleaner()
        String expected = "G01 X10 Y25";

        assertEquals(input, expected);
    }
}
