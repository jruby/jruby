# See JRUBY-3315

# This is quite elegant (IMO), but also VERY slow under MRI 1.8. Run
# under jruby or ruby1.9!

# The problem:
# 
# If a number is even, divide it by two; if it is odd, multiply by
# three and add one. So if we start with the number 13, the sequence
# goes 13 -> 40 -> 20 -> 10 -> 5 -> 16 -> 8 -> 4 -> 2 -> 1. This
# "chain" is 10 links long. (Sequence ends when you reach 1).
# 
# Project Euler #14 wants to know: what number, less than 1_000_000,
# generates the longest chain? (Later numbers in the chain can go over
# 1M, but the first number must be <= 1M.)

# Here's my solution; I am trying for elegant/readable/tight/clean
# code.

require 'benchmark'

def euler14
  h = Hash.new {|h,k| h[k] = (1 + ((k % 2 == 0) ? h[k/2] : h[3*k+1])) }
  h[1] = 1
  puts m = (1..1000000).map {|i| h[i]}.max
end

(ARGV[0] || 5).to_i.times do
  puts Benchmark.measure { euler14 }
end

# This runs in about 100s on Ruby 1.8.
# 
# - Initting a hash with a proc is elegant and clean, but slow. It
#   went from 100s -> 16s after I wrote code to cache the results by
#   hand.
# 
# - Looping with downto instead of using map/max prevents duplication
#   of the array. This results in another 16s -> 4s speed impromevent.
# 
# - Interestingly, in the hash init proc, I noticed that h might be
#   aliasing to the external h variable, so I changed the inner
#   variables to |hash,key|. It certainly did have an effect: this
#   *added* 20sec to the execution time. Curious.
