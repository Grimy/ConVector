package drawall;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.*;

/**
 *
 * @author natha
 */
public class DraWall {

	public static void main(String... args) {
		// TODO: parse args instead of hard-coding this
		BufferedReader in;
		PrintStream out;
		try {
			in = new BufferedReader(new FileReader("_.gcode"));
			out = new PrintStream(new File("_.glc"));
		} catch (Exception e) {
			// TODO: handle system exceptions properly
			System.err.println("Can't find file _.gcode");
			System.exit(1);
			return;
		}

		new GCodeCleaner(in, out, "G00|G01");
	}
}
