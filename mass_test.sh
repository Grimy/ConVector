#!/bin/sh

if [ "$#" -ne 1 ] || ! [ -d "$1" ]
then
	echo "Usage: $0 test_directory/" >&2
	exit 1
fi

if [ -d "$1"output ]
then
	rm -R "$1"output
fi

mkdir "$1"output

trace="$1"output/log

rm $trace 2> /dev/null
nb_svg_files=`ls -1 "$1"*.svg | wc -l` 2> /dev/null
nb_ps_files=`ls -1 "$1"*.ps | wc -l` 2> /dev/null

echo "$nb_svg_files files will be imported."

echo "\nExporting files in SVG..."
echo "\n\n*** exporting in SVG ***" >> $trace
printf '%0.s_' $(seq 1 $nb_svg_files)
echo ""

mkdir "$1"output/svg
for file in "$1"*.svg
do
	if [ -f $file ]
	then
		fullname=$(basename "$file")
		filename="${fullname%.*}"
		echo "\n*** $fullname to svg ***" >> $trace 2>&1
		echo -n "-"
		make ARGS="$file "$1"output/svg/$(basename "$filename.svg")" run >> $trace 2>&1
	fi
done

echo "\n\nExporting files in PS..."
echo "\n\n*** exporting in PS ***" >> $trace
printf '%0.s_' $(seq 1 $nb_svg_files)
echo ""

mkdir "$1"output/ps
for file in "$1"*.svg
do
	if [ -f $file ]
	then
		fullname=$(basename "$file")
		filename="${fullname%.*}"
		echo "\n*** $fullname to ps ***" >> $trace 2>&1
		echo -n "-"
		make ARGS="$file "$1"output/ps/$(basename "$filename.ps")" run >> $trace 2>&1
	fi
done

mkdir "$1"output/pdf
echo "\n\nExporting files in PDF..."
echo "\n\n*** exporting in PDF ***" >> $trace
printf '%0.s_' $(seq 1 $nb_svg_files)
echo ""

for file in "$1"*.svg
do
	if [ -f $file ]
	then
		fullname=$(basename "$file")
		filename="${fullname%.*}"
		echo "\n*** $fullname to pdf ***" >> $trace 2>&1
		echo -n "-"
		make ARGS="$file "$1"output/pdf/$(basename "$filename.pdf")" run >> $trace 2>&1
	fi
done

#mkdir "$1"output/gcode
#echo "\n\nExporting files in GCODE..."
#echo "\n\n*** exporting in GCODE ***" >> $trace
#printf '%0.s_' $(seq 1 $nb_svg_files)
#echo ""
#
#for file in "$1"*.svg
#do
#	if [ -f $file ]
#	then
#		fullname=$(basename "$file")
#		filename="${fullname%.*}"
#		echo "\n*** $fullname to gcode ***" >> $trace 2>&1
#		echo -n "-"
#		make ARGS="$file "$1"output/gcode/$(basename "$filename.gcode")" run >> $trace 2>&1
#	fi
#done

echo "\n\nCreating comparison pictures..."
printf '%0.s_' $(seq 1 $nb_svg_files)
echo ""

mkdir "$1"output/compare
for file in "$1"*.svg
do
	if [ -f $file ]
	then
		fullname=$(basename "$file")
		filename="${fullname%.*}"
		echo -n "-"
		montage -background black \
		\( -border 1 "$file" \) \
		\( -border 1 "$1"output/svg/$filename.svg \) \
		\( -border 1 "$1"output/ps/$filename.ps \) \
		\( -border 1 "$1"output/pdf/$filename.pdf \) \
		-geometry 1280x720+1+1 "$1"output/compare/$filename.png
	fi
done

echo "\ndone"
