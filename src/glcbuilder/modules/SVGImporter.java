package modules;

import java.io.*;
import java.util.Collection;
import model.Instruction;

import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.fop.svg.PDFTranscoder;

/**
 */
public class SVGImporter implements Module {
	@Override
	public String getParamTypes() {
		return "[]";
	}

	@Override
	public String getSupportedFormats() {
		return "svg";
	}

	@Override
	public String getDescription() {
		return "Draws a vector image.";
	}

	@Override
	public Collection<Instruction> process(final InputStream input) throws IOException, TranscoderException {
		Process process = new ProcessBuilder("pstoedit", "-f", "gcode").start();
		new PDFTranscoder().transcode(new TranscoderInput(input),
				new TranscoderOutput(process.getOutputStream()));
		process.getOutputStream().close();
		return new GCodeImporter().process(process.getInputStream());
	}
}
