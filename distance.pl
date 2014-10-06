#!perl -p
# Compute the total distance travelled without drawing for a given GCode file
/G00 X(.*?) Y(.*)/?($\,$x,$y)=($\+sqrt(($1-$x)**2+($2-$y)**2),$1,$2):0}{
