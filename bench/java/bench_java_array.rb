require 'benchmark'
require 'java'

(ARGV[0] || 1).to_i.times {
  Benchmark.bm(30) {|bm|
    bm.report("control, ruby array") {
      a = [1,2,3]
      1_000_000.times {
        a[0]; a[0]; a[0]; a[0]; a[0]
        a[0]; a[0]; a[0]; a[0]; a[0]
      }
    }
    bm.report("java array") {
      a = [1,2,3].to_java :int
      1_000_000.times {
        a[0]; a[0]; a[0]; a[0]; a[0]
        a[0]; a[0]; a[0]; a[0]; a[0]
      }
    }
  }
}
