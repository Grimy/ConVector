package modules;

import java.util.Collection;
import model.Instruction;

/**
 * This module draw a picture with a single line, all the black pixels in the picture (after processing a filter witch
 * convert the picture in B&W colors) are joined with each others with a Travelling salesman algorithm.
 *
 */
public class TravellingSalesman implements Module {

	@Override
	public String getParamTypes() {
		return "[{'name':'Width (in pixels)','type':'int','min':10};" +
				"{'name':'Height (in pixels)','type':'int','min':10}]";
	}

	@Override
	public String getSupportedFormats() {
		return "png,bmp";
	}

	@Override
	public String getDescription() {
		return "Draw a picture with a single line, using a travelling salesman algorithm.";
	}

	@Override
	public Collection<Instruction> process(String inputFilePath) {
		// TODO
		// using convert :
		// http://www.imagemagick.org/script/command-line-options.php#black-threshold
		// or http://www.imagemagick.org/script/command-line-options.php#monochrome
		// or something else better
		throw new UnsupportedOperationException();
	}
}
