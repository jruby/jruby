module Shootout
   extend self

   # Ackermann function
   def ack(m, n)
      if m == 0 then
         n + 1
      elsif n == 0 then
         ack(m - 1, 1)
      else
         ack(m - 1, ack(m, n - 1))
      end
   end

   # Sieveof Eratosthenes
   def sieve(max)
      sieve = []
      2.upto(max){ |i| sieve[i] = i }

      2.upto(Math.sqrt(max)){ |i|
         next unless sieve[i]
         (i*i).step(max, i){ |j|
            sieve[j] = nil
         }
      }
      sieve
   end

   # Floating point harmonic
   def harmonic(max)
      partial_sum = 0
      1.upto(max){ |i|
         partial_sum += (1.0 / i)
      }
      partial_sum
   end
end
