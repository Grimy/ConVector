DOV Specifications
==================

Rationale
---------

DOV (Drawall Optimized Vector) is a binary vectorial image format. It is based
on the following design rationales:
* An image is not usually edited with a text editor. Thus, using a text-driven
format gives no advantage. A binary format is more efficient in terms of both
memory and CPU usage.
* Since vector images can be scaled arbitrarily, the magnitude of coordinates
doesn’t matter, only their relative values. Thus, using floating-point numbers
doesn’t make sense.
* 8-bits integer are too limited. 32-bits are too wasteful. Thus, 16-bits
integers are used to store coordinates. That’s still enough to be within
1/32th-pixel on retina displays and millimeter-accurate on a 13m wall.
* An image is typically written once but displayed many times. Thus, simplicity
of interpretation is more important than expressive power. In particular, the
conversion of complex objects (text, clips…) to simple drawing primitives
should be handled when writing the file, not when reading it.

The last point is particularly important, and begs the question of *which*
drawing primitives to use. Ideally, the selected set of primitives should
allow drawing anything that can be drawn with other vector formats
while being extremely easy to parse.

Drawing model
-------------

Images are represented as *a set of non-overlapping areas*. This avoids the
complexities of clipping, stroke style, line joins, alpha blending, XOR mode
(and on and on) while still allowing to represent arbitrary drawings. For
example, text can be represented using an area for each character, stroked
paths by a thin area… even embedded raster images could be represented, using a
square area for each individual pixel (however, there are better ways to
vectorize an image).

Only two questions remain: how to delimit areas, and how to color them.

Arbitrary curves are not useful in practice. NURBS are very nice, but slightly
too complex for our purpose. As a compromise, DOV allows straight lines
but uses a format allowing possible future additions.

Likewise, DOV only supports flat colors, but adding gradients would be easy.

Details
-------

For consistency, pairs of 16-bits unsigned integers are used for *everything*.
This allows treating the entire file a single array, which simplifies
implementation and should give better cache performance. This is way more
important than shaving a byte here and there.

A DOV file starts with a 16-bytes header, containing the following fields:
* Magic number (0x2339, 0xFFAF), used to identify the DOV format
* Reserved field. Should contain only 0 (0x0000, 0x0000). May be used in a future version.
* Dimensions of the image (width, height)
* Dimension of the original image (width, height)

For maximal accuracy, DOV images should make use of the full range of possible
uint16 values, meaning that either “Width” or “Height” should be 65535.
However, this is not a requirement of the format.

In order to allow converting DOV back to a vector format where the units are
not arbitrary (e.g. GCode), the dimensions of the original image can be stored.
To keep the header size fixed, those original dimensions are not optional. When
they are unknown or meaningless, the fields should contain 0.

Following the header is a sequence of instructions. Each instruction is can be either:
* A point (x, y), with x ≠ 0xFFFF and y ≠ 0xFFFF
* A drawing directive (0xFFFF, code), with code ≠ 0xFFFF.

At present, only the following instruction codes are defined:
Number | Meaning
0x0001 | Move       (move to the next point without drawing)
0xC010 | Set Color  (interpret the next instruction as an RGBA color)

Codes not defined here are reserved for future extensions.

Lines are drawn using the most recently set color.

Formal grammar (EBNF)
--------------------

DOV file ::= header, { instruction };
header ::= magic number, reserved, dimensions, dimensions;
magic number ::= "\x2339", "\xFFAF";
reserved ::= "\x0000", "\x0000";
dimesions ::= uint16, uint16;
uint16 ::= ? any 16-bit unsigned integer ?;

instruction ::= point | "\xFFFF", directive
directive ::= move | set color

point ::= abscissa, ordinate;
abscissa ::= ? a 16-bit unsigned integer other than 0xFFFF ?
ordinate ::= ? a 16-bit unsigned integer other than 0xFFFF ?

move ::= "\x0001"
set color ::= "\xC010", red green, blue alpha;
red green ::= uint16;
blue alpha ::= uint16;

Modules
=======

GCodeBuilder can create GLC from a large variety of files, including GCode and images. Each module
accepts can convert some file types to a common internal representation, which is then converted
to GLC by the GCodeBuilder itself. The correct module is automatically determined based on the
type of the input file. When several modules are available for a file, the user has to choose
which one to use with command line-options.

Examples:
glcbuilder blah.gcode        # Uses the GCodeImporter module
	glcbuilder --shaky blah.png  # Uses the ShakyLines module
glcbuilder blah.png          # Uses some reasonable default for png files (Vectorizer?)


	Guidelines
	----------

	When creating a module, Postel’s law applies: *Be conservative in what you
	send, be liberal in what you accept*.

	In this regard, standards are more like guidelines. Being fully ISO or W3C or
	IILAR compliant may give warm fuzzy feelings, but will achieve nothing in
	practice. What really matters is correct communication with other applications,
	*even* if they are not standards-compliant.

	More specifically:
	* Input modules should, as much as possible, correctly process files generated
	by common applications;
	* Output modules should generate files that can be read by common applications.
