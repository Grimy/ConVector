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

/**
 * A simple wrapper for pstoedit.
 */
public enum VectorImporter implements Module {

	PS,
	SVG {
		@Override
		protected void pipe(InputStream in, OutputStream out) throws TranscoderException {
			new EPSTranscoder().transcode(new TranscoderInput(in),
					// new TranscoderOutput(out));
					new TranscoderOutput(System.out));
			// new SVGTranscoder().transcode(new TranscoderInput(new InputStreamReader(in)),
		}
	};

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
		Process process = null;
		try {
			process = new ProcessBuilder("pstoedit", "-f", "gcode").start();
			pipe(input, process.getOutputStream());
			process.getOutputStream().close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (TranscoderException e) {
			throw new RuntimeException(e);
		}
		return new GCodeImporter().process(process.getInputStream());
	}

	protected void pipe(InputStream in, OutputStream out) throws IOException, TranscoderException {
		int n;
		byte[] buffer = new byte[4096];
		while ((n = in.read(buffer)) >= 0) {
			out.write(buffer, 0, n);
		}
	}
}
