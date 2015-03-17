#!/usr/bin/perl -p
# Compute the total distance travelled for a given file
/.*\D(\d+)\D+(\d+)/?($\,$x,$y)=($\+sqrt(($1-$x)**2+($2-$y)**2),$1,$2):0}{
BEGIN{$==pop}$\=$-=$\/65535*$=
