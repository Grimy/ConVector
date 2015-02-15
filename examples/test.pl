#!/usr/bin/perl -lw
for my $file (<*.svg>) {
	print ": $file | ../convector.jar |> java -ea -jar ../convector.jar %f %o |> $_/$file.$_ {$file}"
		for qw(svg ps pdf);
	print ": $file {$file} |> montage -background white ",
		(map "\\( -border 1 '$_' \\) ", $file, "svg/$file.svg", "ps/$file.ps", "pdf/$file.pdf"),
		"-geometry 1280x720+1+1 %o || true |> montage/$file.png";
}
