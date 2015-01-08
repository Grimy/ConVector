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

package cc.drawall.svg;

import java.awt.Color;

import cc.drawall.Exporter;

/** Outputs a vector as SVG. */
public class SVGExporter extends Exporter {

	/** Constructor. */
	public SVGExporter() {
		super(0, "M%,%", "L%,%", "Q%,% %,%", "C%,% %,% %,%", "Z");
	}

	@Override
	protected void writeHeader(final double width, final double height, final double ratio) {
		write("<?xml version='1.0' encoding='UTF-8' standalone='no'?>\n");
		write("<!DOCTYPE svg PUBLIC '-//W3C//DTD SVG 1.0//EN' ");
		write("'http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd'>\n");
		write("<!-- Superbly generated by ConVector -->\n");
		write("<svg xmlns='http://www.w3.org/2000/svg' transform='scale(%f)' ", ratio);
		write("width='%f' height='%f'", width, height);
		write(">\n<metadata id='ConVector");
	}

	@Override
	protected void writeColor(final Color color) {
		write("'/><path style='fill:#%06x;stroke:none' d='", color.getRGB() & 0xFFFFFF);
	}

	@Override
	protected void writeFooter() {
		write("'/></svg>\n");
	}
}
