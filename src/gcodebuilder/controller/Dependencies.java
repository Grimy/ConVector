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

public class Dependencies {
	private static String OS = System.getProperty("os.name").toLowerCase();

	public static String get_pstoedit_path() {
		String pstoedit_path;
		switch (OS) {
		case "win":
			pstoedit_path = "C://Program Files/pstoedit/pstoedit.exe";
			break;
		case "mac":
			pstoedit_path = "pstoedit";
			break;
		case "nux":
			pstoedit_path = "pstoedit";
			break;
		default:
			pstoedit_path = "pstoedit";
			break;
		}
		return pstoedit_path;
	}

	public static String get_uniconverter_path() {
		String uniconverter_path;
		switch (OS) {
		case "win":
			uniconverter_path = "C://Program Files/uniconverter/uniconverter.exe";
			break;
		case "mac":
			uniconverter_path = "uniconverter";
			break;
		case "nux":
			uniconverter_path = "uniconverter";
			break;
		default:
			uniconverter_path = "uniconverter";
			break;
		}
		return uniconverter_path;
	}
}
