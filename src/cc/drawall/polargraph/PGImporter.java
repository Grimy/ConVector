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

package cc.drawall.polargraph;


import java.awt.geom.Point2D;
import java.nio.channels.ReadableByteChannel;
import java.util.Scanner;

import cc.drawall.Importer;
import cc.drawall.Output;

/** Importer used to parse PostScript. */
public class PGImporter implements Importer {

	private static final int WIDTH = Integer.getInteger("polargraph.width", 7500);

	private static final Point2D polarToCartesian(int a, int b) {
		final double x = (a * a - b * b + WIDTH * WIDTH) / (2 * WIDTH);
		final double y = Math.sqrt(a * a - x * x);
		return new Point2D.Double(x, y);
	}

	@Override
	public void process(final ReadableByteChannel input, final Output output) {
		@SuppressWarnings("resource")
		final Scanner scanner = new Scanner(input, "ascii");
		output.setSize(999, 999);
		scanner.useDelimiter("C|,END(?::.*)?\nC|,");
		boolean penDown = false;
		while (scanner.hasNextInt()) {
			switch (scanner.nextInt()) {
			case 2:
				break;
			case 13:
				penDown = true;
				break;
			case 14:
				penDown = false;
				break;
			case 9:
			case 17:
				final Point2D p = polarToCartesian(scanner.nextInt(), scanner.nextInt());
				output.writeSegment(penDown ? 1 : 0, p.getX(), p.getY());
				break;
			default:
				assert false;
			}
		}
	}
}
