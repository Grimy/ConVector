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

/**
 * The *Lines* module draws a bitmap image with lines.
 *
 */
public class ShakyLines implements Module {

	@Override
	public void process(String inputFilePath, String outputFilePath) {
		// TODO Implement the method using the python code below.
	}

	@Override
	public String getParamTypes() {
		return "[{'name':'nbLines','type':'int','min':10}]";
	}

	@Override
	public String getSupportedFormats() {
		return "png,bmp";
	}

	@Override
	public String getName() {
		return "Shaky lines";
	}
}

/*
def __init__(self):
	self.pos = (0, 0)
	img_path = args.input_file.name # todo: faire un peu mieux

	img = Image.open(img_path)
	self.bitmap = np.array(img)
	self.img_size = img.size
	self._trace()

def _trace(self):
	ngc_path = 'output.ngc' if args.output_file == None else args.output_file.name
	with open(ngc_path, 'w') as gcode_file:
		gcode_file.write('G0 X0 Y0 Z0\nG1\n')
		for line in reversed(self.bitmap):
			for bit in line:
				gcode_file.write(self._draw(True if bit<128 else False))

def _draw(self, isPixel):
	if self.pos[0] < self.img_size[0] - 1:
		if isPixel:
			self.pos = (self.pos[0] + 0.5, self.pos[1] + 0.5)
			line = _get_line(self.pos)
			self.pos = (self.pos[0] + 0.5, self.pos[1] - 0.5)
			line += _get_line(self.pos)
		else:
			self.pos = (self.pos[0] + 1, self.pos[1])
			line = _get_line(self.pos)
	else:		
		self.pos = (0, self.pos[1] + 1)
		line = 'Z25\n'
		line += _get_line(self.pos)
		line += 'Z0\n'
	
	return line

def _get_line(pos):
	return 'X' + str(pos[0]) + ' Y' + str(pos[1]) + '\n'

Lines()
# converted_img.show()
*/
