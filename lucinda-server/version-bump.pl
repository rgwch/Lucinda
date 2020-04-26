#!/usr/bin/perl

# USage: perl version-bump.pl <file with version number>

use strict;
use warnings;

my @VER=<>;
my $VERSION=$VER[0];
$VERSION =~ s/^\s+|\s+$//g;


bumpfile("Dockerfile","lucinda-server-.+?\.jar ","lucinda-server-$VERSION.jar ");
bumpfile("Dockerfile","VERSION=.+? ","VERSION=$VERSION ");
bumpfile("pom.xml","<!-- x --><version>.+</version>","<!-- x --><version>$VERSION</version>");
bumpfile("src/main/java/ch/rgw/lucinda/Restpoint.kt","val LUCINDAVERSION = \".+\"","val LUCINDAVERSION = \"$VERSION\"");

sub bumpfile{
    open(FILE, "<$_[0]") || die "cant open $_[0], $!";
    my @lines = <FILE>;
    close(FILE);

    open(BACKUP, ">$_[0].bak") || die ("can't create $_[0].bak $!");
    print BACKUP @lines;
    close(BACKUP);

    foreach (@lines) {
        s/$_[1]/$_[2]/; # do the replacement
    }

    open(FILE, ">$_[0]") || die "can't write $_[0], $!";
    print FILE @lines;
    close(FILE);
}
