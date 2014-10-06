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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ServiceLoader;
import java.util.logging.Logger;


public enum DrawMaster {;
	private static final Logger log = Logger.getLogger(DrawMaster.class.getName());

	private static final Iterable<Importer> importers = ServiceLoader.load(Importer.class);
	private static final Iterable<Exporter> exporters = ServiceLoader.load(Exporter.class);

	public static Drawing importFile(final String filename) {
		WriterGraphics2D g = new WriterGraphics2D();
		final String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
		log.warning("Importing file " + filename);
		log.warning("Looking for importers for " + extension);
		for (final Importer importer: importers) {
			if (importer.getClass().getSimpleName().replace("Importer", "").toLowerCase().equals(extension)) {
				try (final InputStream input = new FileInputStream(filename)) {
					importer.process(input, g);
					return g.getDrawing();
				} catch (IOException e) {
					log.severe(e.getLocalizedMessage());
					log.finer(e.toString());
				}
			}
		}
		log.severe("Could’t find a suitable importer");
		return null;
	}

	public static void exportFile(final String filename, final Drawing drawing) {
		final String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
		for (final Exporter exporter: exporters) {
			if (exporter.getClass().getSimpleName().replace("Exporter", "").toLowerCase().equals(extension)) {
				try (final OutputStream output = new FileOutputStream(filename)) {
					exporter.output(drawing, output);
					output.flush();
				} catch (IOException e) {
					log.severe(e.getLocalizedMessage());
					log.finer(e.toString());
				}
				return;
			}
		}
		log.severe("Could’t find a suitable exporter");
	}
}
