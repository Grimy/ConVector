/*
 * This file is part of Drawall, a vertical tracer (aka drawbot) - see http://drawall.cc/
 * Drawall is free software and licenced under GNU GPL v3 : http://www.gnu.org/licenses/
 * Copyright (c) 2012-2014 Nathanaël Jourdane
 */
package modules;

import java.io.IOException;

import controller.Dependencies;

public class ImgToGCode {
	public ImgToGCode() {
		try {
			Process process = new ProcessBuilder(Dependencies.get_pstoedit_path(), "param1", "param2").start();
		} catch (IOException e) {
			System.out.println("Can not lauch pstoedit. Please check the path.");
			e.printStackTrace();
		}
	}
}
