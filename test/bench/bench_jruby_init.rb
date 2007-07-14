require 'java'
require 'benchmark'

Ruby = org.jruby.Ruby
System = java.lang.System

Benchmark.bm(20) { |bench|
  bench.report("100 JRuby runtime inits") {
    100.times { Ruby.newInstance(System.in, System.out, System.err) }
  }
end
