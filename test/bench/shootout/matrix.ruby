#!/usr/bin/ruby
# -*- mode: ruby -*-
# $Id: matrix.ruby,v 1.2 2005-03-23 06:11:41 bfulgham Exp $
# http://shootout.alioth.debian.org/
#
# Contributed by Christopher Williams

n = (ARGV[0] || 60).to_i
size = 30

def mkmatrix(rows, cols)
  count = 0
  Array.new(rows) do |i| 
    Array.new(cols) {|j| count +=1 }
  end
end

def mmult(rows, cols, m1, m2)
  m3 = []
  for i in 0 .. (rows - 1)
    row = []
    for j in 0 .. (cols - 1)
      val = 0
      for k in 0 .. (cols - 1)
        val += m1[i][k] * m2[k][j]
      end
      row << val
    end
    m3 << row
  end
  m3
end

m1 = mkmatrix(size, size)
m2 = mkmatrix(size, size)
mm = []
n.times do
  mm = mmult(size, size, m1, m2)
end
puts "#{mm[0][0]} #{mm[2][3]} #{mm[3][2]} #{mm[4][4]}"
