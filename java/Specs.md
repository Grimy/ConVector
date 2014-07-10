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


