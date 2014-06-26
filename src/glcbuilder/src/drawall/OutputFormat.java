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

package drawall;



public class OutputFormat {
	// SEG_MOVETO  0
	// SEG_LINETO  1
	// SEG_QUADTO  2
	// SEG_CUBICTO 3
	// SEG_CLOSE   4
	public static final String[] GLC = {
		"G00 X% Y%", "G01 X% Y%", "G5.1 I% J% X% Y%", "G05 I% J% P% Q% X% Y%", "",
		"", "M30"
	};
	public static final String[] SVG = {
		"M %, %", "L %,%", "Q %,% %,%", "C %,% %,% %,%", "z",
		"<?xml version='1.0' encoding='UTF-8' standalone='no'?>\n" +
		"<!DOCTYPE svg PUBLIC '-//W3C//DTD SVG 1.0//EN' 'http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd'>\n" +
		"<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 300 300'>",
		"</svg>"
	};
}
