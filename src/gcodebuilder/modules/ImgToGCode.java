/*
 * This file is part of Drawall, a vertical tracer (aka drawbot) - see http://drawall.cc/
 * Drawall is free software and licenced under GNU GPL v3 : http://www.gnu.org/licenses/
 * Copyright (c) 2012-2014 Nathanaël Jourdane
 */
package modules;

import java.io.IOException;

import controller.Dependencies;

// commands:
// uniconvertor dessin.svg dessin.ps
// pstoedit dessin.ps dessin.gcode -f gcode

public class ImgToGCode {
	
	public ImgToGCode(String input_file_path, String output_file_path) {
		String ps_tmp_file_path = System.getProperty("java.io.tmpdir") +
				System.getProperty("file.separator") + "drawall_ps";

		try {
			Process process = new ProcessBuilder(Dependencies.get_uniconverter_path(),
					input_file_path, ps_tmp_file_path).start();
		} catch (IOException e) {
			System.out.println("Can not lauch pstoedit. Please check the path.");
			e.printStackTrace();
		}

		try {
			Process process = new ProcessBuilder(Dependencies.get_pstoedit_path(), ps_tmp_file_path,
					output_file_path).start();
		} catch (IOException e) {
			System.out.println("Can not lauch pstoedit. Please check the path: it's actually "
						+ Dependencies.get_pstoedit_path() + ", is it correct ?");
			e.printStackTrace();
		}
	}
}
