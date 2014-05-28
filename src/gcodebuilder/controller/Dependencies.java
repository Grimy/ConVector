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

	public static String getPstoeditPath() {
		String pstoeditPath;
		switch (OS) {
		case "win":
			pstoeditPath = "C://Program Files/pstoedit/pstoedit.exe";
			break;
		case "mac":
			pstoeditPath = "pstoedit";
			break;
		case "nux":
			pstoeditPath = "pstoedit";
			break;
		default:
			pstoeditPath = "pstoedit";
			break;
		}
		return pstoeditPath;
	}

	public static String getUniconverterPath() {
		String uniconverterPath;
		switch (OS) {
		case "win":
			uniconverterPath = "C://Program Files/uniconverter/uniconverter.exe";
			break;
		case "mac":
			uniconverterPath = "uniconverter";
			break;
		case "nux":
			uniconverterPath = "uniconverter";
			break;
		default:
			uniconverterPath = "uniconverter";
			break;
		}
		return uniconverterPath;
	}
	
	public static String getPotracePath() {
		String potracePath;
		switch (OS) {
		case "win":
			potracePath = "C://Program Files/potrace/potrace.exe";
			break;
		case "mac":
			potracePath = "potrace";
			break;
		case "nux":
			potracePath = "potrace";
			break;
		default:
			potracePath = "potrace";
			break;
		}
		return potracePath;
	}
}