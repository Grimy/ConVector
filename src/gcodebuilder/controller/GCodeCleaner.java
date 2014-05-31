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
import java.util.Scanner;
import java.util.regex.*;

public class GCodeCleaner {
	private double[] pos = { 0.0, 0.0, 0.0 };
	private double width, height;
	private String mask;

	public GCodeCleaner(String mask) {
		this.mask = mask;
	}

	public GCodeCleaner() {
		this("G00|G01");
	}

	public void clean(BufferedReader in, PrintStream out) {
		Scanner scanner = new Scanner(in);
		while (scanner.hasNextLine()) {
			String cleaned = cleanLine(scanner.nextLine());
			if (!cleaned.isEmpty()) {
				out.println(cleaned);
			}
		}
	}

	private String cleanLine(String line) {
		String cleaned = line;

		// Remove whitespace
		System.out.println(line);
		cleaned = cleaned.replaceAll("[ \\t]", "");
		// Remove comments
		cleaned = cleaned.replaceAll("^;.*", "");
		cleaned = cleaned.replaceAll("\\(.*", "");
		// Insert G01 if the line begins with an argument
		// cleaned = cleaned.replaceAll("([IJKXYZPQ])", " \\1");
		// Insert spaces before each argument
		cleaned = cleaned.replaceAll("([GM])(\\d\\s)", "\\10\\2");

		boolean posChanged = false;
		for (char param = 'X'; param <= 'Z'; ++param) {
			Matcher matcher = Pattern.compile(param + "\\s*([.0-9]+)").matcher(line);
			if (matcher.find()) {
				setPos(param, Double.parseDouble(matcher.toMatchResult().group(1)));
				posChanged = true;
			}
		}

		if (posChanged) {
			cleaned = cleaned.substring(0, 3) + " X" + getPos('X') +
				" Y" + getPos('Y') + " Z" + getPos('Z');
		}

		cleaned = cleaned.replaceAll("^G0[01]$", "");
		return cleaned;
	}

	public double getPos(char param) {
		return pos[param - 'X'];
	}

	public void setPos(char param, double value) {
		pos[param - 'X'] = value;
	}
}
