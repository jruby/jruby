#!/usr/bin/ruby
# -*- mode: ruby -*-
# $Id: spellcheck.ruby,v 1.3 2005-06-21 05:36:55 igouy-guest Exp $
# http://shootout.alioth.debian.org/
# Revised by Dave Anderson 

dict = Hash.new
l = ""

IO.foreach("Usr.Dict.Words") do |l|
  dict[l.chomp!] = 1
end 

STDIN.each do |l|
  unless dict.has_key? l.chomp!
    puts l
  end
end
