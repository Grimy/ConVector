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

package cc.drawall.dov;

import java.nio.channels.ReadableByteChannel;
import java.util.InputMismatchException;
import java.util.Scanner;

import cc.drawall.Canvas;
import cc.drawall.Importer;

/** Importer used to parse GCode. */
public class DOVImporter implements Importer {

	@Override
	public Canvas process(final ReadableByteChannel input) {
		final Canvas g = new Canvas();
		@SuppressWarnings("resource")
		final Scanner scanner = new Scanner(input, "UnicodeBig");
		scanner.useDelimiter("");
		scanner.skip("\u2339\uFFAF");

		final int width = scanner.next().charAt(0);
		final int height = scanner.next().charAt(0);
		g.setSize(width, height);
		final double ratio = Math.max(width, height) / 65535.0;
		g.getTransform().scale(ratio, ratio);
		g.setStrokeWidth((float) (1 / ratio));

		while (scanner.hasNext()) {
			final int x = scanner.next().charAt(0);
			final int y = scanner.next().charAt(0);
			if (x == 0xFFFF && y == 0x0001) {
				g.moveTo(scanner.next().charAt(0), scanner.next().charAt(0));
				continue;
			} else if (x == 0xFFFF || y == 0xFFFF) {
				throw new InputMismatchException();
			}
			g.lineTo(x, y);
		}
		g.fill().resetPath();
		return g;
	}
}
