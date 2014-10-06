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

package cc.drawall;

import java.util.Locale;
import java.util.logging.Logger;

/** Command-line interface. */
public enum GLCBuilder {;

	static {
		// This is necessary so that the decimal separator is "." everywhere.
		// It might become a problem if we want to internationalize the interface.
		Locale.setDefault(Locale.US);
		System.setProperty("java.util.logging.config.file", "bin/cc/drawall/log.properties");
	}
	private static final Logger log = Logger.getLogger("drawall.GLCBuilder");

	// private static final String STD = "-";

	/** Parses command-line arguments, instantiates a GLCBuilder and runs it. */
	public static void main(final String... args) {
		if (args.length == 0) {
			new cc.drawall.gui.GUI().init();
			return;
		}
		if (args.length > 2) {
			log.severe("Too many arguments.");
			return;
		}

		final String inFile = args[0];
		final String outFile = args[1];

		final Drawing drawing = DrawMaster.importFile(inFile);
		DrawMaster.exportFile(outFile, drawing);
	}
}
