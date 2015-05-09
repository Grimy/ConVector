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

import javafx.scene.paint.Color;
import java.awt.Shape;
import java.awt.geom.PathIterator;


/** The base class for all Exporter plugins.
  * Provides a common template for all output filetypes. Abstract methods should be overriden
  * to implement the details relevant to a particular filetype. */
public interface Output {

	/** Writes the beginning of the output file.
	  * @param width the width of the original drawing
	  * @param height the height of the original drawing
	  * @param ratio the scaling ratio that should be applied to this drawing to return
	  * it to its original size */
	void setSize(final double width, final double height);

	void writeColor(final double red, final double green, final double blue);

	void writeSegment(final int type, final double... coords);

	void writeFooter();

	default void paint(final Color color, final Shape shape) {
		final double[] coords = new double[6];
		writeColor(color.getRed(), color.getGreen(), color.getBlue());
		for (final PathIterator i = shape.getPathIterator(null); !i.isDone(); i.next()) {
			writeSegment(i.currentSegment(coords), coords);
		}
	}
}
