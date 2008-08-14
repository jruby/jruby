require 'benchmark'

def bench_flip(bm)
  bm.report("1m x10 while (a)..(!a) (stack)") do
    1_000_000.times do
      a = true
      while (a)..(!a); a = false; end
      a = true
      while (a)..(!a); a = false; end
      a = true
      while (a)..(!a); a = false; end
      a = true
      while (a)..(!a); a = false; end
      a = true
      while (a)..(!a); a = false; end
      a = true
      while (a)..(!a); a = false; end
      a = true
      while (a)..(!a); a = false; end
      a = true
      while (a)..(!a); a = false; end
      a = true
      while (a)..(!a); a = false; end
      a = true
      while (a)..(!a); a = false; end
      a = true
      while (a)..(!a); a = false; end
    end
  end
  bm.report("1m x10 while (a)..(!a) (heap)") do
    1_000_000.times do
      if false; eval ''; end
      a = true
      while (a)..(!a); a = false; end
      a = true
      while (a)..(!a); a = false; end
      a = true
      while (a)..(!a); a = false; end
      a = true
      while (a)..(!a); a = false; end
      a = true
      while (a)..(!a); a = false; end
      a = true
      while (a)..(!a); a = false; end
      a = true
      while (a)..(!a); a = false; end
      a = true
      while (a)..(!a); a = false; end
      a = true
      while (a)..(!a); a = false; end
      a = true
      while (a)..(!a); a = false; end
      a = true
      while (a)..(!a); a = false; end
    end
  end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_flip(bm)} }
end
