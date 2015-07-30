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
 * © 2014-2015 Victor Adam
 */

package cc.drawall;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Logger;

/** User interface. */
final class ConVector {
	static {
		// Ensure the decimal separator is "." everywhere.
		Locale.setDefault(Locale.Category.FORMAT, Locale.US);
		System.setProperty("java.util.logging.config.file", "bin/log.properties");
	}
	private static final Logger log = Logger.getLogger(ConVector.class.getName());

	private ConVector() { /* Utility class */ }

	/** If command lines arguments are given, process them. Otherwise, start the GUI.
	  * @param args first argument is the input file, second is the output file */
	public static void main(final String... args) {
		if (args.length == 0) {
			WebService.loop(3434);
			return;
		}
		final List<String> argv = new ArrayList<>(Arrays.asList(args));
		final Map<String, String> options = new HashMap<>();
		for (Iterator<String> itr = argv.iterator(); itr.hasNext();) {
			String arg = itr.next();
			if (arg.startsWith("--")) {
				int i = arg.indexOf('=');
				i = i == -1 ? arg.length() : i;
				options.put(arg.substring(2, i), arg.substring(i + 1));
				itr.remove();
			}
		}
		argv.stream().map(FileSystems.getDefault()::getPath).reduce((inFile, outFile) -> {
			try (final ReadableByteChannel in = open(inFile, options);
				final FileChannel out = FileChannel.open(outFile, StandardOpenOption.WRITE,
						StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
				options.put("output-type", getExtension(outFile.toString()));
				convert(in, out, options);
			} catch (final IOException e) {
				log.severe("Problem converting " + inFile + " to " + outFile + ": " + e);
			}
			return inFile;
		});
	}
	
	private static ReadableByteChannel open(final Path file, final Map<String, String> options) throws IOException {
		if (!options.containsKey("canny")) {
			options.put("input-type", getExtension(file.toString()));
			return FileChannel.open(file, StandardOpenOption.READ);
		}
		options.put("input-type", "svg");
		final String tmp = File.createTempFile("drawall", ".pbm").getAbsolutePath();
		try {
			new ProcessBuilder("convert", "-colorspace", "gray", "-canny", options.get("canny"),
				file.toString(), tmp).redirectError(Redirect.INHERIT).start().waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return Channels.newChannel(new ProcessBuilder("potrace", "-so-", tmp).redirectError(Redirect.INHERIT).start().getInputStream());
	}

	public static void convert(final ReadableByteChannel in, final WritableByteChannel out, final Map<String, String> options) {
		Output output = new SimpleOutput(out, exporter(options.get("output-type")));
		if (options.containsKey("merge") || options.containsKey("optimize")) {
			output = new Drawing(output, options.containsKey("merge"), options.containsKey("optimize"));
		}
		importer(options.get("input-type")).process(in, output);
		output.writeFooter();
	}

	private static String getExtension(final String filename) {
		return filename.substring(filename.lastIndexOf('.') + 1);
	}


	/** Parses the specified InputStream using a plugin appropriate for the specified filetype
	  * and returns the resulting Drawing.
	  * @param input the channel in which to read the data to be parsed
	  * @param filetype indicates how to interpret read data
	  * @return the resulting vector */
	private static Importer importer(final String filetype) {
		for (final Importer importer: ServiceLoader.load(Importer.class)) {
			final String name = importer.getClass().getSimpleName().replace("Importer", "");
			if (name.equalsIgnoreCase(filetype)) {
				return importer;
			}
		}
		throw new InputMismatchException("No suitable importer found for " + filetype);
	}

	/** Writes a drawing to a stream, using a plugin appropriate for the specified filetype. */
	private static Exporter exporter(final String filetype) {
		for (final Exporter exporter: ServiceLoader.load(Exporter.class)) {
			final String name = exporter.getClass().getSimpleName().replace("Exporter", "");
			if (name.equalsIgnoreCase(filetype)) {
				return exporter;
			}
		}
		throw new InputMismatchException("No suitable exporter found for " + filetype);
	}
}
