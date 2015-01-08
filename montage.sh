#!/bin/sh

die() { echo -e "$@" >&2; exit 1; }

JAVA="java -ea -classpath $(dirname $(readlink -f $0))/bin"

for format in svg ps pdf; do
	$JAVA cc.drawall.ConVector "examples/$1" "output/$1.$format" 2>/dev/null || die
done
montage -background white \
	\( -border 1 "examples/$1" \) \
	\( -border 1 "output/$1.svg" \) \
	\( -border 1 "output/$1.ps"  \) \
	\( -border 1 "output/$1.pdf" \) \
	-geometry 1280x720+1+1 "output/$1.png"
true
