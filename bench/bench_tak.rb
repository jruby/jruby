def tak x, y, z
  if y >= x
    return z
  else
    return tak( tak(x-1, y, z),
                tak(y-1, z, x),
                tak(z-1, x, y))
  end
end
 
require "benchmark"
 
N = (ARGV.shift || 1).to_i
 
Benchmark.bm do |make|
  N.times do
    make.report do
      i = 0
      while i<10
        tak(24, 16, 8)
        i+=1
      end
    end
  end
end
