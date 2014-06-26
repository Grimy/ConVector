#!/usr/bin/perl -p0

use v5.20;
use warnings;
my ($package, $classname) = pop =~ /(.*)\/(.*)/;
$package =~ s!/!.!g;
$package =~ s!^plj!drawall!;
my $type = qr/(?:enum|class|interface) (\w+)/;

my @fqcn = split /\n/, `cat ~/.vim/cache/java/*`;
my %fqcn;

sub prio {
	for (@_) {
		return -1 if not defined;
		return 4*/^$package$/ + 2*/^java\.util/ + /^java\./;
	}
}

sub best {
	my ($new, $old) = @_;
	prio($new) > prio($old) ? $new : $old;
}

for (@fqcn) {
	s!/!.!g;
	s!(.*)\.!!;
	my $pkg = $1;
	$fqcn{$_} = $pkg if prio($pkg) > prio($fqcn{$_});
}

# Ignore strings and comments
my $cleaned = s!"(?:[^"\\]|\\.)*"!!gr =~ s!//.*!!gr =~ s!/\*(?:[^*]|\*[^/])*\*/!!gsrx;
my %classes;
scalar(\@classes{$cleaned =~ /(?<!class )(?<!\.)\b[A-Z]\w*[a-z]\w*/g});
delete $classes{$_} for $cleaned =~ /$type/g;

say <<'LICENSE';
/*
 * This file is part of DraWall.
 * DraWall is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * DraWall is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with DraWall. If not, see <http://www.gnu.org/licenses/>.
 * Copyright (c) 2012-2014 Nathanaël Jourdane.
 */
LICENSE

say "package $package;";
say "";
my @imports = map {
	die "Class not found: $_" if not defined $fqcn{$_};
	$fqcn{$_} ne 'java.lang' && $fqcn{$_} ne $package ? "import $fqcn{$_}.$_;" : ()
} keys %classes;
say for sort @imports;
say "";

s!;\n! $classname !;

