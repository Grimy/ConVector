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

package cc.drawall.svg;

import java.nio.ByteBuffer;

import cc.drawall.Exporter;

/** Outputs a vector as SVG. */
public class SVGExporter extends Exporter {

	public SVGExporter() {
		super("M%,%", "L%,%", "Q%,% %,%", "C%,% %,% %,%", "Z");
	}

	@Override
	protected ByteBuffer header(final double width, final double height, final double ratio) {
		return format("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>\n"
			+ "<!-- Superbly %s -->\n"
			+ "<svg xmlns='http://www.w3.org/2000/svg' width='%f' height='%f'>\n"
			+ "<g transform='scale(%f)' stroke-width='%f' fill='none' "
			+ "stroke='black' stroke-linecap='round' stroke-linejoin='round'><path d='",
			COMMENT, width, height, ratio, .5 / ratio);
	}

	@Override
	protected ByteBuffer color(final double red, final double green, final double blue) {
		return format("'/><path stroke='none' fill='#%02x%02x%02x' d='",
			(int) (red * 255), (int) (green * 255), (int) (blue * 255));
	}

	@Override
	protected ByteBuffer footer() {
		return format("'/></g></svg>\n");
	}
}
