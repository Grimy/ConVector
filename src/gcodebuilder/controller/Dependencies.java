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
		switch(OS) {
		case "win":
			pstoedit_path = "C://...";
			break;
		case "mac":
			pstoedit_path = "...";
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

}
