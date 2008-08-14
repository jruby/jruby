require 'benchmark'

def bench_classvars(bm)
  oldbm = $bm
  $bm = bm
  class << self
    class BenchClassvars
      $bm.report("control, 1m block loops") { 1_000_000.times { self } }
      $bm.report("1m class var decl") { 1_000_000.times { @@foo = self } }
      def self.foo
        $bm.report("1m class var asgn") { 1_000_000.times { @@foo = self } }
      end
      foo
      $bm.report("1m class var") { 1_000_000.times { @@foo; self } }
    end
  end
  $bm = oldbm
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_classvars(bm)} }
end