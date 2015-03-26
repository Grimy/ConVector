export LANG=en_US.UTF-8
original=$1

rm "$original"*.svg
for i in $(seq 2 1 4); do
	for j in $(seq 8 2 28); do
		filename="$original"_"$i"_"$j"
		convert -resize 1920 $original -canny "$i"x"$i"+"$j"%+"$j"% $filename
		potrace -s $filename -o $filename.svg
		sed -i 's/fill="#000000" stroke="none"/fill="none" stroke="#000" stroke-width="10"/g' $filename.svg
		rm $filename
	done
done

