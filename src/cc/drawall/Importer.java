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

/** Base interface for plugins. */
@FunctionalInterface
public interface Importer {
	/** Interprets bytes read from `input` and draws on `output`. Each implementing
	  * class is a way to interpret bytes as a vector image.
	  * @param input the channel in which to read the data to be parsed
	  * @return the resulting vector */
	Canvas process(final ReadableByteChannel input);
}
