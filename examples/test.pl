#!/usr/bin/perl -lw
for my $file (<*.svg>) {
	my @all = ("svg/$file.svg", "ps/$file.ps", "pdf/$file.pdf");
	print ": $file | ../convector.jar |> java -ea -jar ../convector.jar %f %o |> @all {$file}";
	print ": $file {$file} |> montage -background white ",
		(map "\\( -border 1 '$_' \\) ", $file, @all),
		"-geometry 1280x720+1+1 %o || true |> montage/$file.png";
}
