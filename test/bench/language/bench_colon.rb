require 'benchmark'

module Foo; module Bar; end; end


def bench_colon(bm)
  oldbm = $bm
  $bm = bm
  class << self
    module Foo
      $bm.report("control, const access directly") do
        1_000_000.times do
          Foo; Bar; Foo; Bar; Foo; Bar; Foo; Bar; Foo; Bar; Foo; Bar; Foo; Bar; Foo; Bar; Foo; Bar; Foo; Bar
        end
      end
    end
    $bm.report("1m colon2") do
      1_000_000.times do
        Foo::Bar; Foo::Bar; Foo::Bar; Foo::Bar; Foo::Bar; Foo::Bar; Foo::Bar; Foo::Bar; Foo::Bar; Foo::Bar
      end
    end
    class Object
      $bm.report("control, const access from Object") do
        1_000_000.times do
          Foo; Foo; Foo; Foo; Foo; Foo; Foo; Foo; Foo; Foo
        end
      end
      $bm.report("1m colon3") do
        1_000_000.times do
          ::Foo; ::Foo; ::Foo; ::Foo; ::Foo; ::Foo; ::Foo; ::Foo; ::Foo; ::Foo
        end
      end
    end
  end
  $bm = oldbm
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_colon(bm)} }
end