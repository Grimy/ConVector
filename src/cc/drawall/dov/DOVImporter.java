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

import cc.drawall.Graphics;
import cc.drawall.Importer;

/** Importer used to parse GCode. */
public class DOVImporter implements Importer {
	private final Graphics g = new Graphics();
	private Scanner scanner;

	@Override
	public Graphics process(final ReadableByteChannel input) {
		scanner = new Scanner(input, "UnicodeBig");
		scanner.useDelimiter("");
		scanner.skip("\u2339\uFFAF");
		int width = scanner.next().charAt(0);
		int height = scanner.next().charAt(0);
		double ratio = Math.max(width, height) / 65535.0;
		g.getTransform().scale(ratio, ratio);
		g.setStrokeWidth((float) (1 / ratio));
		while (scanner.hasNext()) {
			int x = scanner.next().charAt(0);
			int y = scanner.next().charAt(0);
			System.out.printf("%x, %x\n", x, y);
			if (y == 0xFFFF) {
				throw new InputMismatchException();
			}
			if (x == 0xFFFF) {
				if (y == 0x0001) {
					g.moveTo(false, scanner.next().charAt(0), scanner.next().charAt(0));
					continue;
				}
				throw new InputMismatchException();
			}
			g.lineTo(false, x, y);
		}
		g.draw();
		return g;
	}
}
