[![Build Status](https://travis-ci.org/DraWallPlotter/ConVector.svg?branch=travis)](https://travis-ci.org/DraWallPlotter/ConVector)

ConVector
=========

![The ConVector logo](http://drawall.cc/wordpress/wp-content/uploads/2015/01/convector_logo2.png)

ConVector converts vector images between various formats. At the moment, it supports the following formats:
- DOV (output only)
- SVG
- PostScript
- GCode
- PDF (output only)

For more informations about the DOV format, please see [the specifications](https://github.com/DraWallPlotter/ConVector/blob/master/doc/Specs.md).

Quick Start
===========

- Download the [latest release](https://github.com/DraWallPlotter/ConVector/releases/download/v0.4.1/convector-0.4.1.jar)
- Double-click the downloaded file. If you get an error, you have to install [Java 1.8](https://www.java.com/en/download/manual.jsp)
- Click “Import”
- Pick an image in one of the supported input formats (for example, an SVG file)
- After a small wait, the image should be displayed
- Click “Export”
- Enter a filename, using the extension for the desired output format (for example, "name.dov")

You can also [try ConVector online](http://convector.drawall.cc).

Advanced Use
============

Besides the graphical interface, ConVector can also be run from the command-line. This can be useful for batch processing, or to run ConVector as a web service.
To convert a file :

```sh
java -jar convector.jar inputfile.svg outputfile.dov
```

Note that the extensions are used to guess the filetype of both files. If the output file already exists, it is replaced without warning.

To start a web server :

```sh
java -jar convector.jar 3434
```

You should see the following message:

```
Listening on port 3434
```

You can then point your browser (or curl) to [localhost:3434](http://localhost:3434). Here’s an example curl command :
```sh
curl --data-binary @inputfile.svg localhost:3434/svg/dov > outputfile.dov
```

DraWall Project
===============

ConVector is part of the DraWall project. DraWall is a robot that draws vector graphics on walls (or any vertical surface, actually).
More information (in French) can be found on the [project site](http://www.drawall.cc).
