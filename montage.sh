#!/bin/sh

export MAGICK_THREAD_LIMIT=1 MAGICK_MEMORY_LIMIT=256MiB
ROOT="$(dirname $(readlink -f "$0"))"
cd "$(dirname $(readlink -f "$1"))"
FILE="$(basename "$1")"
mkdir -p pdf ps svg png diff montage
java -ea -jar "$ROOT/convector.jar" "$FILE" "svg/$FILE.svg" "ps/$FILE.ps" "pdf/$FILE.pdf"
convert -antialias -crop $(identify svg/$FILE.svg | cut -d' ' -f3)+0+0 "$FILE" "png/$FILE.png"
ALL="png/$FILE.png svg/$FILE.svg diff/$FILE.png"
SCORE="$(compare -alpha off -metric MAE $ALL 2>&1 | cut -d' ' -f1 | cut -d. -f1)"
printf "%5d %s\n" "$SCORE" "$FILE"
[ "$SCORE" -lt 65 ] || not \
montage -background white $ALL "ps/$FILE.ps" "pdf/$FILE.pdf" -geometry 533x450 "montage/$FILE.png"