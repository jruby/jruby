#!/usr/bin/ruby
# -*- mode: ruby -*-
# $Id: strcat.ruby-2.ruby,v 1.1 2004-11-10 06:44:59 bfulgham Exp $
# http://shootout.alioth.debian.org/

n = Integer(ARGV.shift || 1)

str = ''
for i in 1 .. n
    str += "hello\n"
end
puts str.length
