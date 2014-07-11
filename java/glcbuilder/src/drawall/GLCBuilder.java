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

package drawall;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Locale;

/** Main entry point. */
public class GLCBuilder {

	/** How to output the parsed Instructions. */
	private static String inputFiletype = "";
	private static String outputFiletype = "";
	private static InputStream input = System.in;
	private static PrintStream output = System.out;

	/** Converts `input` to `format` and writes the result to `output`. */
	public static void run() {
		// Output the parsed instructions
		WriterGraphics2D g = pickOutput();
		pickPlugin().process(input, g);
		g.done(output);
	}

	/** Shows an usage message and exits with code `returnCode`. */
	private static void usage(int returnCode) {
		System.err.println("Usage: GLCBuilder [options] [input] [output]");
		System.exit(returnCode);
	}

	/** Parses command-line arguments, instantiates a GLCBuilder and runs it. */
	public static void main(String... args) {
		int i = 0;

		// This is necessary so that the decimal separator is "." everywhere.
		// It might become a problem if we want to internationalize the interface.
		Locale.setDefault(Locale.US);

		// Parse named arguments (how I wish Java had a GetOpts)
		while (i < args.length && args[i].charAt(0) == '-') {
			// TODO: move the body of this while to a method responsible for parsing a single argument
			switch (args[i++]) {
			case "-svg":
				// format = OutputFormat.SVG;
				break;
			default:
				System.err.println("Unknown option: " + args[i]);
				usage(1);
			}
		}

		// Parse positional arguments (ditto)
		try {
			if (args.length > i) {
				String filename = args[i++];
				input = new FileInputStream(filename);
				inputFiletype = filename.substring(filename.lastIndexOf('.') + 1);
			}
			if (args.length > i) {
				String filename = args[i++];
				output = new PrintStream(filename);
				outputFiletype = filename.substring(filename.lastIndexOf('.') + 1);
			}
		} catch (FileNotFoundException e) {
			System.err.println("Cannot open file " + args[i - 1]);
			System.exit(2);
		}
		if (args.length > i) {
			System.err.println("Too many arguments.");
			usage(1);
		}

		try {
			run();
		} catch (Exception e) {
			e.printStackTrace(); // XXX this is for debugging only
			System.exit(3);
		}
	}

	/** Pick a plugin capable of interpreting the input’s inputFiletype.
	  * TODO: find a better way to detect inputFiletype than the extension.
	  * Extensions technically mean nothing (easy to modify), and STDIN doesn’t have an extension. */
	private static Plugin pickPlugin() {
		switch (inputFiletype) {
		case "ngc":
		case "glc":
		case "gcode":
			return new GCodeImporter();
		case "svg":
			input = new SVGtoPS(input); // $FALL-THROUGH$
		case "ps":
			return new PSImporter();

		default:
			System.err.println("Unsupported file type : " + inputFiletype);
			System.exit(3);
			return null;
		}
	}

	private static WriterGraphics2D pickOutput() {
		switch (outputFiletype) {
			case "ngc":
			case "glc":
			case "gcode":
				return new GCodeOutput();
			case "svg":
				return new SVGOutput();

			default:
				System.err.println("Unsupported file type : " + outputFiletype);
				System.exit(3);
				return null;
		}
	}
}
