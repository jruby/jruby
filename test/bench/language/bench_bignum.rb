require 'benchmark'

(ARGV[0] || 10).to_i.times {
  Benchmark.bm(30) {|bm|
    bm.report("control: 1m block with self") { 1_000_000.times { self }}
    bm.report("1m bignums") { 1_000_000.times { 111_111_111_111_111_111_111_111_111_111 }}
  }
}