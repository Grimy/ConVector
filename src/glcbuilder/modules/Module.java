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

package modules;

import java.util.Collection;
import java.io.InputStream;
import java.io.IOException;
import model.Instruction;

public interface Module {
	public String getParamTypes();
	public String getSupportedFormats();
	public String getDescription();

	public Collection<Instruction> process(InputStream input) throws Exception;
}

/* modules parameters:
 * name, type, min, max, values
 *
 * int -> spinner
 * int with min and max -> cursor
 * int with values -> listbox
 * String -> textbox
 * boolean -> checkbox
 */
