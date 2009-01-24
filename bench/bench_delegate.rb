require 'benchmark'
require 'delegate'

TIMES = (ARGV[0] || 1).to_i
TIMES.times {
  Benchmark.bm(30) {|bm|
    obj1 = ''
    obj2 = Object.new
    def obj2.to_str(); 'foo'; end
    dele1 = SimpleDelegator.new(obj1)
    dele2 = SimpleDelegator.new(obj2)
    bm.report("control, direct calls, native") { 10_000_000.times { obj1.to_str }}
    bm.report("SimpleDelegate wrapper, native") { 10_000_000.times { dele1.to_str }}
    bm.report("control, direct calls, ruby") { 10_000_000.times { obj2.to_str }}
    bm.report("SimpleDelegate wrapper, ruby") { 10_000_000.times { dele2.to_str }}
  }
}
