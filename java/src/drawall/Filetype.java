/*
 * This file is part of DraWall.
 * DraWall is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * DraWall is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with DraWall. If not, see <http://www.gnu.org/licenses/>.
 * © 2012–2014 Nathanaël Jourdane
 * © 2014 Victor Adam
 */

package drawall;

import java.util.HashMap;
import java.util.Map;


public enum Filetype {
	GCODE,
	PS,
	SVG,
	PDF;

	public static final Map<String, Filetype> map = new HashMap<>();
	static {
		map.put("ngc", GCODE);
		map.put("glc", GCODE);
		map.put("ngc", GCODE);
		map.put("svg", SVG);
		map.put("ps",  PS);
		map.put("pfa", PS);
		map.put("pfb", PS);
		map.put("gsf", PS);
		map.put("pdf", PDF);
	}

	public static Filetype fromFilename(String filename) {
		Filetype result = map.get(filename.substring(filename.lastIndexOf('.') + 1).toLowerCase());
		assert result != null : "Unsupported file type : " + filename;
		return result;
	}

	/** Pick a plugin capable of interpreting the input’s filetype. */
	public Plugin input() {
		switch (this) {
			case GCODE:
				return new GCodeImporter();
			case PS:
				return new PSImporter();
			case SVG:
				return new SVGImporter();
			case PDF:
			default:
				assert false;
				return null;
		}
	}

	public Output output() {
		switch (this) {
			case GCODE:
				return new GCodeOutput();
			case SVG:
				return new SVGOutput();
			case PDF:
				return new PDFOutput();
			case PS:
			default:
				assert false;
				return null;
		}
	}
}
