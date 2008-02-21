require 'benchmark'

module Foo
  (ARGV[0] || 10).to_i.times {
    class << self
      Benchmark.bm(30) {|bm|
        bm.report("1m aliases") { 1000000.times { alias xxx to_s } }
        bm.report("1m alias_methods") { 1000000.times { alias_method :yyy, :to_s } }
      }
    end
    Benchmark.bm(30) {|bm|
      bm.report("control: 1m to_s on topself") { 1000000.times { to_s } }
      bm.report("1m alias'ed calls") { 1000000.times { xxx } }
      bm.report("1m alias_method'ed calls") { 1000000.times { yyy } }
    }
  }
end