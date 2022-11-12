#!/usr/bin/perl

print "What's your name? ";

my $name = <STDIN>;

chomp $name;

print "Hello ", $name, "!\n";

print "What's your favourite colour? ";

my $colour = <STDIN>;

chomp $colour;

print "Your favourite colour is '$colour'.\n";

print "GCC version: ", `gcc --version`, "\n";
print "G++ version: ", `g++ --version`, "\n";

1;
