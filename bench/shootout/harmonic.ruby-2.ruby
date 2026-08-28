#!/usr/bin/ruby
# http://shootout.alioth.debian.org/
#
# Contributed by Christopher Williams
# modified by Daniel South
# modified by Doug King

n = (ARGV[0] || 10000000).to_i

partialSum = 0.0
for i in (1..n)
  partialSum += (1.0 / i)
end

printf("%.9f\n", partialSum)
