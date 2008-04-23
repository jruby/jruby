require 'benchmark'
require 'bigdecimal'

puts "BigDecimal#sqrt with 100 digit precision"

5.times {
  puts Benchmark.measure {
    10000.times{ BigDecimal('2').sqrt(100) }
  }
}

puts "BigDecimal#div with 1000 digit precision"

5.times {
  puts Benchmark.measure {
    one = BigDecimal('1')
    three = BigDecimal('3')
    10000.times {
      one.div(three, 1000)
    }
  }
}
