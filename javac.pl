#!/usr/bin/perl -lw

use strict;

my $CLASSPATH='-classpath bin';
my $JAVAC="javac $CLASSPATH -sourcepath src -source 1.8 -d bin";
my @sources = split "\n", `find src/cc -name '*.java'`;
my @classes = map s/java$/class/r, map s/^src/bin/r, @sources;
push @classes, 'bin/cc/drawall/Drawing$Splash.class', 'bin/cc/drawall/ps/PSImporter$PSDict.class';

print ": @sources |> LANG=en_US.UTF-8 $JAVAC %f |> @classes {bin}";
