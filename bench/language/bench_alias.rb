require 'benchmark'

def bench_alias(bm)
  oldbm = $bm
  $bm = bm
  # this is to avoid errors about module def in method body
  class << self
    module Foo
      class << self
        $bm.report("1m aliases") { 1000000.times { alias xxx to_s } }
        $bm.report("1m alias_methods") { 1000000.times { alias_method :yyy, :to_s } }
      end
      $bm.report("control: 1m to_s on topself") { 1000000.times { to_s } }
      $bm.report("1m alias'ed calls") { 1000000.times { xxx } }
      $bm.report("1m alias_method'ed calls") { 1000000.times { yyy } }
    end
  end
  $bm = oldbm
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(30) {|bm| bench_alias(bm)} }
end