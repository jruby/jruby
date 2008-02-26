require 'benchmark'

(ARGV[0] || 10).to_i.times do
  Benchmark.bm(30) do |$bm|
    class Foo
      $bm.report("control, 1m block loops") { 1_000_000.times { self } }
      $bm.report("1m class var decl") { 1_000_000.times { @@foo = self } }
      def self.foo
        $bm.report("1m class var asgn") { 1_000_000.times { @@foo = self } }
      end
      foo
      $bm.report("1m class var") { 1_000_000.times { @@foo; self } }
    end
  end
end