require 'benchmark'
require 'bigdecimal'

5.times {
  puts Benchmark.measure {
    10000.times{BigDecimal('2').sqrt(100)}
  }
}
