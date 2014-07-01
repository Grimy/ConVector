/*
 * This file is part of DraWall.
 * DraWall is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * DraWall is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with DraWall. If not, see <http://www.gnu.org/licenses/>.
 * Copyright (c) 2012-2014 Nathanaël Jourdane.
 */

package drawall;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;

public abstract class Output {
	public void start() { /* do nothing */ }
	public void startPath(Color color) { /* do nothing */ }
	public void draw(int type, double[] coords) { /* do nothing */ }
	public void endPath() { /* do nothing */ }
	public void end() { /* do nothing */ }
	public void draw(Area a, AffineTransform transform, int flatness, Color color) {
		double[] coords = new double[6];
		startPath(color);
		PathIterator itr = flatness < 0 ? a.getPathIterator(transform) : a.getPathIterator(transform, flatness);
		for (; !itr.isDone(); itr.next()) {
			draw(itr.currentSegment(coords), coords);
		}
		endPath();
	}
}
