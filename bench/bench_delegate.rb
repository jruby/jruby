require 'benchmark'
require 'delegate'

TIMES = (ARGV[0] || 1).to_i
TIMES.times {
  Benchmark.bm(30) {|bm|
    obj = ''
    dele = SimpleDelegator.new(obj)
    bm.report("control, direct calls") { 10_000_000.times { obj.to_str }}
    bm.report("SimpleDelegate wrapper") { 10_000_000.times { dele.to_str }}
  }
}