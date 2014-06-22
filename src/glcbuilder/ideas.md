Writer.java
===========
I have absolutely no idea how to use .ttf files to draw something, but this module should be very fun.

Vectorizer.java
===============
Use Potrace or http://en.wikipedia.org/wiki/Canny_edge_detector

TravellingSalesman.java
=======================
/* This module draw a picture with a single line, all the black pixels in the picture (after processing a filter witch
 * convert the picture in B&W colors) are joined with each others with a Travelling salesman algorithm.
 * TODO
 * using convert :
 * http://www.imagemagick.org/script/command-line-options.php#black-threshold
 * or http://www.imagemagick.org/script/command-line-options.php#monochrome
 * or something else better
 */

ShakyLines.py
=============
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

ConfigParser.java
=================

public class ConfigParser {

	public static String dependenciesFileName = "drawall_dependencies.properties";

	/**
	 * Save properties to the project’s root folder.
	 */
	public void writeProperties() {
		Properties prop = new Properties();
		prop.setProperty("database", "localhost");
		prop.setProperty("dbuser", "mkyong");
		prop.setProperty("dbpassword", "password");

		try (FileOutputStream output = new FileOutputStream(dependenciesFileName)) {
			prop.store(output, null);
		} catch (IOException e) {
			// TODO: handle this
		}
	}

	public void readProperties() {
		Properties prop = new Properties();

		try (FileInputStream input = new FileInputStream(dependenciesFileName)) {
			prop.load(input);
		} catch (IOException e) {
			// TODO: handle this
		}

		// get the property value and print it out
		System.out.println(prop.getProperty("database"));
		System.out.println(prop.getProperty("dbuser"));
		System.out.println(prop.getProperty("dbpassword"));
	}
}
