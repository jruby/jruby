# Simple quicksort benchmark, once for a small array and once for a very large array
require 'benchmark'
require 'bench_quicksort_data'

def quicksort(l)
  if l == [] then 
    []
  else
    x, *xs = l

    quicksort(xs.select { |i| i <  x }) + [x] + 
      quicksort(xs.select { |i| i >= x })
  end
end

Benchmark.bm(25) { |b|
  b.report("500-element quicksort") { quicksort($small_array) }
  b.report("140k-element quicksort") { quicksort($big_array) }
}
Benchmark.bm(25) { |b|
  b.report("500-element quicksort") { quicksort($small_array) }
  b.report("140k-element quicksort") { quicksort($big_array) }
}
