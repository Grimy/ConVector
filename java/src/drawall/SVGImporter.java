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

import java.awt.Graphics2D;
import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.io.StringReader;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.fop.render.ps.EPSTranscoder;

/** Uses Batik to convert SVG to PS. */
public class SVGImporter implements Plugin {

	@Override
	public void process(Reader input, Graphics2D g) {
		// XXX this is terrible performance-wise
		ByteArrayOutputStream charOutput = new ByteArrayOutputStream();
		try {
			new EPSTranscoder().transcode(new TranscoderInput(input), new TranscoderOutput(charOutput));
		} catch (TranscoderException e) {
			throw new RuntimeException(e);
		}
		new PSImporter().process(new StringReader(charOutput.toString()), g);
	}
}
