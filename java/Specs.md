GLCBuilder generates a strict subset of GCode, called GLC (or Glucose for short). GLC is what the
DrawBot is (or will be) able to interpret and execute.

GLC Specifications
==================

Each line consists of one *operation code* followed by any number of *arguments*, separated by
single spaces. The arguments’ names must match exactly those required by the operationc code.

An *argument* is a single uppercase letter (its *name*) followed by a the decimal representation
of a floating point number.

An *operation code* is one of the following:

Code Arguments Meaning
G00  X Y Z     Move in a line without drawing
G01  X Y Z     Draw a line
G02† X Y I J   Draw an arc, clockwise
G03† X Y I J   Draw an arc, counterclockwise
G04  P         Wait P seconds
M30            End the program
M00†           Pause

† Those codes are not yet understood by the DrawBot.
ASK: do we really need a Z argument?

Notable differences from GCode
==============================

* No variables
* No mathematical expressions
* No optional arguments
* No implicit motion mode
* No comments or excessive whitespace
* Most operations are forbidden (usually because they don’t make sense in the
context of DrawBot, i.e. drilling)
* Single digit version of the codes (i.e. G0) are not allowed (use G00)
* All coordinate are always absolute

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



DOV Specifications
==================

Rationale
=========

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
=============

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
and cubic Béziers curves, and uses a format allowing possible future additions.

Likewise, DOV only supports flat colors, but adding gradients will be easy.

Details
=======

For consistency, 16-bits unsigned integers are used for *everything*. This
allows treating the entire file a single uint16 array, which simplifies
implementation and should give better cache performance. This is way more
important than shaving a byte here and there.

A DOV file starts with a 12-bytes header, containing the following fields:
* Magic number 14847 (0x39FF), used to identify the DOV format
* Reserved field. Should contain 0 (0x0000). May be used in a future version.
* Width
* Height
* Original image’s width
* Original image’s height

For maximal accuracy, DOV images should make use of the full range of possible
uint16 values, meaning that either “Width” or “Height” should be 65535.
However, this is not a requirement of the format.

In order to allow converting DOV back to a vector format where the units are
not arbitrary (e.g. GCode), the dimensions of the original image can be stored.
To keep the header size fixed, those original dimensions are not optional. When
they are unknown or meaningless, the fields should contain 0.

Following the header are instructions. Each instruction is followed by arguments.
Number and meaning of arguments depend on the instruction.

Defined instructions are:
Number | Meaning     | Arguments
0x0001 | Linear Area | 2 + 2n (see below)
0x0003 | Cubic Area  | 8 + 6n (see below)
0xC010 | Set Color   | 2 (a 32-bit RGBA color code, split in two uint16)
0xFFFF | End         | 0
Codes not defined here are reserved for future extensions.

Areas are filled using the most recently set color.
The arguments to area instructions describe a list of points. Each point is
given by a pair of coordinates: (x, y). The number of points in an area is not
known in advance: the interpreter should keep on reading points until the area
is closed (i.e. until the last point read is the same as the first point read).

A linear area is simply a polygon whose corners are the given points.

Cubic areas are bounded by Bézier splines. The first four points describe the
first Bézier curve (start point, control points, end point, in this order). For
subsequent curves, only control and end points are given: the start point is
implicitly the same as the end point of the previous curve.

Note that this doesn’t allow mixing straight lines and Bézier curves. Straight lines
can still be represented by Béziers curves with all points aligned.

At the end of the file, a CRC16 checksum can optionally be appended.

Formal grammar (EBNF)
====================

DOV file ::= header, { instruction }, footer;
header ::= magic number, reserved, width, height, original width, original height;
magic number ::= "\x39FF";
reserved ::= "\x0000";
width ::= uint16;
height ::= uint16;
uint16 ::= ? any 16-bit unsigned integer ?;

instruction ::= set color | linear area | cubic area;

set color ::= "C010", red green, blue alpha;
red green ::= uint16;
blue alpha ::= uint16;

linear area ::= "\x0001", point, { point };
cubic area ::= "\x0003", point, { point, point, point };
point ::= abscissa, ordinate;
abscissa ::= uint16;
ordinate ::= uint16;

footer ::= end marker, [ CRC ];
end marker ::= "\xFFFF";
CRC ::= uint16;

