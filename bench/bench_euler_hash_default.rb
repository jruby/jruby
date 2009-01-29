require 'benchmark'

(ARGV[0] || 1).to_i.times do
  puts Benchmark.measure {
    h = Hash.new {|h,k| h[k] = (1 + ((k % 2 == 0) ? h[k/2] : h[3*k+1])) }
    h[1] = 1
    (1..1000000).map {|i| h[i]}.max
  }
end
