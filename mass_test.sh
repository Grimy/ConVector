#!/bin/sh

die() { echo -e "$@" >&2; exit 1; }

progress_bar() {
	printf '\r%3s [' $format
	printf '%-56.56s' $(head -c $(expr $i \* 56 / $nb_files) </dev/zero | tr '\0' '#')
	printf '] %-17.17s' $file
	i=$(expr $i + 1)
}

JAVA="java -ea -classpath $(dirname $(readlink -f $0))/bin"

cd "$1" || die "Usage: $0 test_directory/"
rm -r output/* 2>/dev/null
mkdir output

nb_files=$(find * -maxdepth 0 -type f | wc -l) 2>/dev/null
echo "$nb_files files will be converted."

for format in svg ps pdf; do
	i=1
	for file in $(find * -maxdepth 0 -type f); do
		basename="${file%.*}"
		progress_bar
		$JAVA cc.drawall.ConVector "$file" output/$basename.$format || die
	done
	echo
done

echo "Creating comparison pictures..."

mkdir output/compare
for file in $(find * -maxdepth 0 -type f); do
	basename="${file%.*}"
	echo -n "-"
	montage -background black \
		\( -border 1 "$file" \) \
		\( -border 1 output/$basename.svg \) \
		\( -border 1 output/$basename.ps \) \
		\( -border 1 output/$basename.pdf \) \
	-geometry 1280x720+1+1 output/compare/$basename.png
done

echo "\ndone"
