require 'java'
require 'benchmark'

import java.lang.Math

Benchmark.bm(40) {|bm|
  # Math.abs computation cost is negligible for these tests
  (ARGV[0] || 5).to_i.times {
    bm.report("Control") {
      m = Math
      value = 1
      10_000_000.times { m; value }
    }
    bm.report("Math.abs with byte-ranged Fixnum") {
      m = Math
      value = 1
      10_000_000.times { m.abs(value) }
    }
    bm.report("Math.abs with short-ranged Fixnum") {
      m = Math
      value = 1 << 8
      10_000_000.times { m.abs(value) }
    }
    bm.report("Math.abs with int-ranged Fixnum") {
      m = Math
      value = 1 << 16
      10_000_000.times { m.abs(value) }
    }
    bm.report("Math.abs with long-ranged Fixnum") {
      m = Math
      value = 1 << 32
      10_000_000.times { m.abs(value) }
    }
    bm.report("Math.abs with float-ranged Float") {
      m = Math
      value = 2.0
      10_000_000.times { m.abs(value) }
    }
    bm.report("Math.abs with double-ranged Float") {
      m = Math
      value = 2.0 ** 128
      10_000_000.times { m.abs(value) }
    }
  }
}