require 'benchmark'

p RUBY_DESCRIPTION

N = 1299709 #100K-th prime
PRIMES = (2..N).to_a
5.times do
  primes = PRIMES.dup
  Benchmark.bm(30) { |bm|
    bm.report("100K primes") do
      index = 0
      while primes[index]**2 < primes.last
        prime = primes[index]
        primes = primes.select {|x| x == prime || x%prime != 0}
        index += 1
      end
    end
  }
end
