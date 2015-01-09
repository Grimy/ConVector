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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.InputMismatchException;
import java.util.ServiceLoader;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

/** Responsible for loading plugins and invoking the correct one for a given filetype. */
enum PluginOverseer {;
	private static final Logger log = Logger.getLogger(PluginOverseer.class.getName());

	/** Parses the specified InputStream using a plugin appropriate for the specified
	  * filetype, and returns the resulting Drawing. */
	static Drawing importStream(final FileChannel input) throws IOException {
		for (final Importer importer: ServiceLoader.load(Importer.class)) {
			log.info("Trying to import using " + importer.getClass());
			try {
				return importer.process(input).drawing;
			} catch (final InputMismatchException e) {
				log.warning(importer.getClass() + ": " + e);
			}
			input.position(0);
		}
		log.severe("No suitable importers found");
		return new Drawing();
	}

	/** Writes a drawing to a stream, using a plugin appropriate for the specified filetype. */
	static void exportStream(final ByteBuffer output, final String filetype,
			final Drawing drawing) {
		StreamSupport.stream(ServiceLoader.load(Exporter.class).spliterator(), false).filter(
			o -> o.getClass().getSimpleName().replace("Exporter", "").equalsIgnoreCase(filetype)
		).findAny().get().output(drawing, output);
	}
}
