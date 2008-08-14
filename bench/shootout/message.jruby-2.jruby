#!/usr/bin/env ruby
#
# Gonzalo Garramuno -- Dec.31 2006
#

def coroutine(n)
  if n > 1
    coroutine(n-1) { |x| yield x + 1 }
  else
    yield 1 while true
  end
end

iter  = 0
last  = ARGV[0].to_i
count = 0

coroutine( 500 ) { |x|
  break if iter >= last
  count += x
  iter  += 1
}

puts count
