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

import java.util.Vector;

/** PostScript’s graphical state stack.
  * Acts as a singly linked stack. Clients should only keep a reference to the top item. */
public class PSGraphics {

	/** The previously saved graphical state. */
	PSGraphics prev = null;

	/** Current Transformation Matrix.
	  * The actual matrix is 3x3, but only 6 coefficients are stored;
	  * the last column is always {0, 0, 1}. Thus, the array {a, b, c, d, e, f}
	  * represents the matrix
	  * / a b 0 \
	  * | c d 0 |
	  * \ e f 1 /
	  */
	double[] ctm = idMatrix();

	/** Current path. */
	Vector<Instruction> path = new Vector<>();

	//** Current clip path. (XXX: unimplemented) */

	/** Current linecap. 0 = buttcap, 1 = round cap, 2 = projecting cap. */
	byte linecap = 0;

	/** Current line join. 0 = Miter join, 1 = round join, 2 = Bevel join. */
	byte linejoin = 0;

	/** Current Miter limit. Below this angle, a Bevel join is used instead of a Miter join. */
	double miterLimit = 10;

	/** Line width for stroked paths. */
	double linewidth = 1;

	/** Saves the current graphical state. */
	PSGraphics dup() {
		// TODO: insert the copy in the second position in the stack,
		// so that clients don’t have to switch pointers
		PSGraphics clone = new PSGraphics();
		clone.ctm = ctm.clone();
		clone.path.addAll(path);
		clone.linecap = linecap;
		clone.linejoin = linejoin;
		clone.miterLimit = miterLimit;
		clone.linewidth = linewidth;
		clone.prev = this;
		return clone;
	}

	/** Returns a matrix representing the identity transform. */
	static double[] idMatrix() {
		return new double[]{1.0, 0.0, 0.0, 1.0, 0.0, 0.0};
	}
}
