#!/usr/bin/ruby
# -*- mode: ruby -*-
# $Id: wc.ruby,v 1.1.1.1 2004-05-19 18:13:51 bfulgham Exp $
# http://www.bagley.org/~doug/shootout/
# with help from Paul Brannan

nl = nw = nc = 0
loop do
  data = (STDIN.read(4096) or break) << (STDIN.gets || "")
  nc += data.length
  nl += data.count("\n")
  ((data.strip! || data).tr!("\n", " ") || data).squeeze!
  nw += data.count(" ") + 1
end
puts "#{nl} #{nw} #{nc}"
