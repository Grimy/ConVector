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
 * © 2014-2015 Victor Adam
 */

package cc.drawall;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Logger;

/** Responsible for loading plugins and invoking the correct one for a given filetype. */
enum PluginOverseer {;
	private static final Logger log = Logger.getLogger(PluginOverseer.class.getName());
	private static final Map<String, Importer> importers = new HashMap<>();
	private static final Map<String, Exporter> exporters = new HashMap<>();
	static {
		for (Importer importer: ServiceLoader.load(Importer.class)) {
			importers.put(importer.getClass().getSimpleName().replace("Importer", "")
					.toLowerCase(), importer);
		}
		for (Exporter exporter: ServiceLoader.load(Exporter.class)) {
			exporters.put(exporter.getClass().getSimpleName().replace("Exporter", "")
					.toLowerCase(), exporter);
		}
		log.info("Done importing plugins");
	}

	/** Parses the specified InputStream using a plugin appropriate for the specified
	  * filetype, and returns the resulting Drawing. */
	static Drawing importStream(final ReadableByteChannel input, final String filetype) {
		return importers.get(filetype).process(input).drawing;
	}

	/** Writes a drawing to a stream, using a plugin appropriate for the specified filetype. */
	static void exportStream(final ByteBuffer output, final String filetype,
			final Drawing drawing) {
		exporters.get(filetype).output(drawing, output);
	}
}
