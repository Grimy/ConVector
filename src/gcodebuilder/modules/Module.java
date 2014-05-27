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

public abstract class Module {
	private String input_file_name;
	private String output_file_name;
	private String files_types;
	private String params;

	Module(String input_file_name, String output_file_name, String files_types) {
		this(input_file_name, output_file_name, files_types, "");
	}

	Module(String input_file_name, String output_file_name, String files_types, String params) {
		this.input_file_name = input_file_name;
		this.output_file_name = output_file_name;
		this.files_types = files_types;
		this.params = output_file_name;
	}
}
