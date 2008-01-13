require 'benchmark'

(ARGV[0] || 5).to_i.times {
  puts Benchmark.measure {
    x = 0
    while x < 1_000_000
      def a; 1 + 1; end
      x += 1
    end
  }
}
