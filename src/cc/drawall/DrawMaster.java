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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;


public enum DrawMaster {;

	public static Drawing importStream(final InputStream input, final String filetype) {
		WriterGraphics2D g = new WriterGraphics2D();
		StreamSupport.stream(ServiceLoader.load(Importer.class).spliterator(), false).filter(
			o -> o.getClass().getSimpleName().replace("Importer", "").equalsIgnoreCase(filetype)
		).findAny().get().process(input, g);
		return g.getDrawing();
	}

	public static void exportStream(final OutputStream output, final String filetype,
			final Drawing drawing) throws IOException {
		StreamSupport.stream(ServiceLoader.load(Exporter.class).spliterator(), false).filter(
			o -> o.getClass().getSimpleName().replace("Exporter", "").equalsIgnoreCase(filetype)
		).findAny().get().output(drawing, output);
		output.flush();
	}
}
