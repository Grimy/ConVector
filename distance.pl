#!perl -p
# Compute the total distance travelled without drawing for a given GCode file
/G0[01] X(.*?) Y(.*)/?($\,$x,$y)=($\+sqrt(($1-$x)**2+($2-$y)**2),$1,$2):0}{
BEGIN{$==pop}$\=$-=$\/65535*$=
