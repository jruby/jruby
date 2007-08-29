#!/usr/bin/ruby
# -*- mode: ruby -*-
# $Id: strcat.ruby,v 1.1.1.1 2004-05-19 18:13:35 bfulgham Exp $
# http://www.bagley.org/~doug/shootout/
# based on code from Aristarkh A Zagorodnikov and Dat Nguyen

STUFF = "hello\n"
hello = ''
(ARGV.first.to_i || 1).times do
    hello << STUFF
end
puts hello.length
