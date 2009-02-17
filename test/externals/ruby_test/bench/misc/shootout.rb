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
   
   # Fibonacci sequence
   def fib(n)
      if n > 1 then
        fib(n - 2) + fib(n - 1)
      else
        1
      end
   end   

   # A fannkuch (pancake) number flipping function. See
   # http://www.apl.jhu.edu/~hall/text/Papers/Lisp-Benchmarking-and-Fannkuch.ps
   def fannkuch(n)
      max_flips, m, r, check = 0, n-1, n, 0
      count = (1..n).to_a
      perm = (1..n).to_a

      while true
         if check < 30
            check += 1
         end

         while r != 1:
            count[r-1] = r
            r -= 1
         end

         if perm[0] != 1 and perm[m] != n
            perml = perm.clone
            flips = 0
            while (k = perml.first ) != 1
               perml = perml.slice!(0, k).reverse + perml
               flips += 1
            end
            max_flips = flips if flips > max_flips
         end
         
         while true
            if r==n : return max_flips end
            perm.insert r,perm.shift
            break if (count[r] -= 1) > 0
            r += 1
         end
      end
   end
end
