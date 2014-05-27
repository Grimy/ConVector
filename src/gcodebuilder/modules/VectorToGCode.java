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

package modules;

import java.io.IOException;

import controller.Dependencies;

// commands:
// uniconvertor dessin.svg dessin.ps
// pstoedit dessin.ps dessin.gcode -f gcode

public class VectorToGCode {
	
	public VectorToGCode(String input_file_path, String output_file_path) {
		String ps_tmp_file_path = System.getProperty("java.io.tmpdir") +
				System.getProperty("file.separator") + "drawall_ps";

		try {
			Process process = new ProcessBuilder(Dependencies.get_uniconverter_path(),
					input_file_path, ps_tmp_file_path).start();
		} catch (IOException e) {
			System.out.println("Can not lauch pstoedit. Please check the path: it's actually "
					+ Dependencies.get_uniconverter_path() + ", is it correct ?");
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
