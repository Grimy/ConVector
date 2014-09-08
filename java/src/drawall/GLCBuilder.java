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

import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Locale;

/** Main entry point. */
public class GLCBuilder {

	public static void run(Reader input, PrintWriter output,
			Filetype inputFiletype, Filetype outputFiletype) {
		// Output the parsed instructions
		WriterGraphics2D g = new WriterGraphics2D();
		inputFiletype.input().process(input, g);
		outputFiletype.output().output(g.getColorMap(), g.getNormalizingTransform(), output);
		output.flush();
	}

	/** Shows an usage message and exits with code `returnCode`. */
	private static void usage(int returnCode) {
		System.err.println("Usage: GLCBuilder [options] [input] [output]");
		System.exit(returnCode);
	}

	/** Parses command-line arguments, instantiates a GLCBuilder and runs it. */
	public static void main(String... args) {
		// This is necessary so that the decimal separator is "." everywhere.
		// It might become a problem if we want to internationalize the interface.
		Locale.setDefault(Locale.US);

		String inputFilename = "-", outputFilename = "-";
		int i = 0;

		// Parse named arguments (how I wish Java had a GetOpts)
		while (i < args.length && args[i].charAt(0) == '-') {
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
		if (args.length > i) {
			inputFilename = args[i++];
		}
		if (args.length > i) {
			outputFilename = args[i++];
		}
		if (args.length > i) {
			System.err.println("Too many arguments.");
			usage(1);
		}

		try (Reader input = inputFilename.equals("-") ?
				new InputStreamReader(System.in) : new FileReader(inputFilename);
				PrintWriter output = outputFilename.equals("-") ?
				new PrintWriter(System.out) : new PrintWriter(outputFilename)) {
			run(input, output, Filetype.fromFilename(inputFilename),
					Filetype.fromFilename(outputFilename));
		} catch (Exception e) {
			e.printStackTrace(); // XXX this is for debugging only
			System.exit(3);
		}
	}
}
