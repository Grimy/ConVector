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

public class VectorImporter implements Module {

	@Override
	public void process(String inputFilePath, String outputFilePath) {
		String psTmpFilePath = System.getProperty("java.io.tmpdir") +
				System.getProperty("file.separator") + "drawall_ps";

		try {
			Process process = new ProcessBuilder(Dependencies.getUniconverterPath(),
					inputFilePath, psTmpFilePath).start();
			process.waitFor();
			// TODO catch uniconverter error
		} catch (IOException e) {
			throw new RuntimeException("Can not lauch uniconverter. Please check the path: it's currently "
					+ Dependencies.getUniconverterPath() + ", is it correct ?");
		} catch (InterruptedException e) {
			throw new RuntimeException("Process uniconverter aborted.");
		}

		try {
			Process process = new ProcessBuilder(Dependencies.getPstoeditPath(), psTmpFilePath,
					outputFilePath).start();
			process.waitFor();
			// TODO catch pstoedit error
		} catch (IOException e) {
			throw new RuntimeException("Can not lauch pstoedit. Please check the path: it's currently "
						+ Dependencies.getPstoeditPath() + ", is it correct ?");
		} catch (InterruptedException e) {
			throw new RuntimeException("Process pstoedit aborted.");
		}
	}

	@Override
	public String getParamTypes() {
		return "[]";
	}

	@Override
	public String getSupportedFormats() {
		return "svg,ps";
	}
	
	@Override
	public String getName() {
		return "VectorToGCode";
	}

	@Override
	public String getDescription() {
		return "Draw a vector image.";
	}

}
