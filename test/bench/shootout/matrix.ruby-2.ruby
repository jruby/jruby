#!/usr/bin/ruby
# -*- mode: ruby -*-
# $Id: matrix.ruby-2.ruby,v 1.1 2005-03-23 06:11:41 bfulgham Exp $
# http://shootout.alioth.debian.org/
#
# Contributed by Christopher Williams

n = (ARGV[0] || 60).to_i
size = 30
require 'matrix'
n = (ARGV[0] || 600).to_i
size = 30

def mkmatrix(rows,cols)
  count = 0
  the_rows = Array.new(rows) do |i| 
    Array.new(cols) {|j| count +=1 }
  end
  Matrix[*the_rows]
end

m1 = mkmatrix(size,size)
m2 = mkmatrix(size,size)
mm = []
n.times do
  mm = m1 * m2
end
puts "#{mm[0,0]} #{mm[2,3]} #{mm[3,2]} #{mm[4,4]}"


