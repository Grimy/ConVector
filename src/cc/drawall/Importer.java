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

import java.nio.channels.ReadableByteChannel;
import java.util.Locale;
import java.util.ServiceLoader;

/** Base interface for plugins. */
@FunctionalInterface
public interface Importer {
	/** Interprets bytes read from `input` and draws on `output`.
	  * Each implementing class is a way to interpret bytes as a vector image.
	  * @param input the channel in which to read the data to be parsed
	  * @return the resulting vector */
	Graphics process(final ReadableByteChannel input);

	/** Parses the specified InputStream using a plugin appropriate for the specified
	 * filetype, and returns the resulting Drawing. */
	static Drawing importStream(final ReadableByteChannel input, final String filetype) {
		for (Importer importer: ServiceLoader.load(Importer.class)) {
			if (importer.getClass().getSimpleName().replace("Importer", "")
					.toLowerCase(Locale.US).equals(filetype)) {
				return importer.process(input).drawing;
			}
		}
		return null;
	}
}
