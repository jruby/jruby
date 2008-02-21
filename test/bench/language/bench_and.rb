require 'benchmark'

(ARGV[0] || 10).to_i.times {
  Benchmark.bm(20) {|bm|
    bm.report("100k * 100 true && true") {
      100000.times {
        true && true; true && true; true && true; true && true; true && true
        true && true; true && true; true && true; true && true; true && true
        true && true; true && true; true && true; true && true; true && true
        true && true; true && true; true && true; true && true; true && true
        true && true; true && true; true && true; true && true; true && true
        true && true; true && true; true && true; true && true; true && true
        true && true; true && true; true && true; true && true; true && true
        true && true; true && true; true && true; true && true; true && true
        true && true; true && true; true && true; true && true; true && true
        true && true; true && true; true && true; true && true; true && true
        true && true; true && true; true && true; true && true; true && true
        true && true; true && true; true && true; true && true; true && true
        true && true; true && true; true && true; true && true; true && true
        true && true; true && true; true && true; true && true; true && true
        true && true; true && true; true && true; true && true; true && true
        true && true; true && true; true && true; true && true; true && true
        true && true; true && true; true && true; true && true; true && true
        true && true; true && true; true && true; true && true; true && true
        true && true; true && true; true && true; true && true; true && true
        true && true; true && true; true && true; true && true; true && true
      }
    }
    
    bm.report("100k * 100 false && true") {
      100000.times {
        false && true; false && true; false && true; false && true; false && true
        false && true; false && true; false && true; false && true; false && true
        false && true; false && true; false && true; false && true; false && true
        false && true; false && true; false && true; false && true; false && true
        false && true; false && true; false && true; false && true; false && true
        false && true; false && true; false && true; false && true; false && true
        false && true; false && true; false && true; false && true; false && true
        false && true; false && true; false && true; false && true; false && true
        false && true; false && true; false && true; false && true; false && true
        false && true; false && true; false && true; false && true; false && true
        false && true; false && true; false && true; false && true; false && true
        false && true; false && true; false && true; false && true; false && true
        false && true; false && true; false && true; false && true; false && true
        false && true; false && true; false && true; false && true; false && true
        false && true; false && true; false && true; false && true; false && true
        false && true; false && true; false && true; false && true; false && true
        false && true; false && true; false && true; false && true; false && true
        false && true; false && true; false && true; false && true; false && true
        false && true; false && true; false && true; false && true; false && true
        false && true; false && true; false && true; false && true; false && true
      }
    }
  }
}