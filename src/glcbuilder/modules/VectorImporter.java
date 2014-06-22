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

import java.io.*;
import java.util.Collection;
import model.Instruction;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.svg2svg.SVGTranscoder;
import org.apache.fop.svg.PDFTranscoder;
import org.apache.fop.render.ps.EPSTranscoder;

public enum VectorImporter implements Module {

	PS,
	SVG;

	@Override
	public String getParamTypes() {
		return "[]";
	}

	@Override
	public String getSupportedFormats() {
		return "ps,pdf";
	}

	@Override
	public String getDescription() {
		return "Draws a vector image.";
	}

	@Override
	public Collection<Instruction> process(InputStream input) {
		if (this == SVG) {
			TranscoderInput tin = new TranscoderInput(input);
			PipedInputStream pin = new PipedInputStream();
			input = pin;
			new Thread(() -> {
				try {
					PipedOutputStream pout = new PipedOutputStream(pin);
					TranscoderOutput tout = new TranscoderOutput(pout);
					new EPSTranscoder().transcode(tin, tout);
				} catch (IOException | TranscoderException e) {
					throw new RuntimeException(e);
				}
			}).start();
		}
		return new Parser().process(input);
	}
}
