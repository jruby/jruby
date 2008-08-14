# The Computer Language Shootout
# http://shootout.alioth.debian.org/
#
# Contributed by Jesse Millikan

require 'thread'

N = ARGV[0].to_i
next_q = last_q = SizedQueue.new(1)

500.times {
 q = SizedQueue.new(1)
 q2 = next_q
 Thread.new{
  i = N
  while i > 0
   q2.push(q.pop+1)
   i -= 1
  end
 }
 next_q = q
}

Thread.new{N.times{next_q.push(0)}}

t = 0
N.times{t+=last_q.pop}
puts t

