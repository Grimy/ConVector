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

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

public class InstructionList implements Iterable<Instruction> {
	private Collection<Instruction> delegate = new Vector<>();

	public void add(Instruction el) {
		delegate.add(el);
	}

	@Override
	public Iterator<Instruction> iterator() {
		return delegate.iterator();
	}
}
