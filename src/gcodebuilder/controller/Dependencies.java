/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

/**
 * 
 * @author natha
 */
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
