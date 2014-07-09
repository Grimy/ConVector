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

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.fop.render.ps.EPSTranscoder;

/** Uses Batik to convert SVG to PS. */
public class SVGtoPS extends PipedInputStream {

	public SVGtoPS(InputStream input) {
		TranscoderInput tin = new TranscoderInput(input);
		// XXX: is it really better to use a Thread?
		new Thread(() -> {
			try {
				TranscoderOutput tout = new TranscoderOutput(new PipedOutputStream(SVGtoPS.this));
				new EPSTranscoder().transcode(tin, tout);
			} catch (IOException | TranscoderException e) {
				throw new RuntimeException(e);
			}
		}).start();
	}
}
