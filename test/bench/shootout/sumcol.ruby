#!/usr/bin/ruby
# -*- mode: ruby -*-
# $Id: sumcol.ruby,v 1.2 2004-11-10 06:43:14 bfulgham Exp $
# http://www.bagley.org/~doug/shootout/
# from: Mathieu Bouchard, revised by Dave Anderson

count = 0
l=""
STDIN.each{ |l|
    count += l.to_i
}
puts count
