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
import java.util.regex.Pattern;

import javafx.scene.paint.Color;

import cc.drawall.Canvas;
import cc.drawall.Importer;

/** Importer used to parse PostScript. */
public class PGImporter implements Importer {

	private static final int WIDTH = Integer.getInteger("polargraph.width");

	private static final Point2D polarToCartesian(int a, int b) {
		final double x = (a * a - b * b + WIDTH * WIDTH) / (2 * WIDTH);
		final double y = Math.sqrt(a * a - x * x);
		return new Point2D.Double(x, y);
	}

	private final Canvas g = new Canvas();

	@Override
	public Canvas process(final ReadableByteChannel input) {
		final Scanner scanner = new Scanner(input, "ascii");
		g.setColor(Canvas.Mode.FILL, null);
		g.setColor(Canvas.Mode.STROKE, Color.BLACK);
		g.setSize(999, 999);
		g.getTransform().scale(999.0 / WIDTH, 999.0 / WIDTH);
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
				if (penDown) {
					g.lineTo((float) p.getX(), (float) p.getY());
				} else {
					g.stroke().resetPath();
					g.moveTo((float) p.getX(), (float) p.getY());
				}
				break;
			default:
				assert false;
			}
		}
		return g;
	}
}
