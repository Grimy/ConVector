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

import java.io.*;
import java.util.Collection;
import model.Instruction;

/**
 * A simple wrapper for pstoedit.
 */
public class PSImporter implements Module {
	@Override
	public String getParamTypes() {
		return "[]";
	}

	@Override
	public String getSupportedFormats() {
		return "ps,pdf";
	}

	@Override
	public String getDescription() {
		return "Draws a vector image.";
	}

	@Override
	public Collection<Instruction> process(InputStream input) throws IOException {
		Process process = new ProcessBuilder("pstoedit", "-f", "gcode").start();
		Pipe.pipe(input, process.getOutputStream());
		return new GCodeImporter().process(process.getInputStream());
	}
}
