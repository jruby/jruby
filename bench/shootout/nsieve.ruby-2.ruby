# The Computer Language Shootout
# http://shootout.alioth.debian.org/
# contributed by Pavel Valodzka

def nsieve(m)
  is_prime = Array.new(m, true)
  count = 0
  2.upto(m){|i|
    if is_prime[i]
      (2 * i).step(m, i){|v|
        is_prime[v] = false
      }
      count += 1
    end
  }
  return count
end

n = (ARGV[0] || 2).to_i
n = 2 if (n < 2)

3.times {|t|
  m = (1<<n-t)*10000
  printf("Primes up to %8d%9d\n", m, nsieve(m))  
}
