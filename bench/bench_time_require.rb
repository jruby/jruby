require 'benchmark'

TIMES = (ARGV[0] || 3).to_i
Benchmark.bm(30) {|bm|
  TIMES.times {
    bm.report("require 'time'") { 10_000.times { require 'time' } }
  }
}

puts "Now with rubygems and activerecord"
require 'rubygems'
gem 'activerecord'
require 'activerecord'

Benchmark.bm(30) {|bm|
  TIMES.times {
    bm.report("require 'time'") { 10_000.times { require 'time' } }
  }
}
