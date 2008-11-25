require 'benchmark'


def bench_colon(bm)
  oldbm = $bm
  $bm = bm
  class << self
    class ::Object
      module Foo
        def self.a; end
        module Bar; module Gar; module Har; end; end; end
        $bm.report("control, const access directly") do
          1_000_000.times do
            Foo; Bar; Foo; Bar; Foo; Bar; Foo; Bar; Foo; Bar; Foo; Bar; Foo; Bar; Foo; Bar; Foo; Bar; Foo; Bar
          end
        end
        $bm.report("1m colon2 (constant 2)") do
          1_000_000.times do
            Foo::Bar; Foo::Bar; Foo::Bar; Foo::Bar; Foo::Bar; Foo::Bar; Foo::Bar; Foo::Bar; Foo::Bar; Foo::Bar
          end
        end
        $bm.report("1m colon3 colon2 (constant)") do
          1_000_000.times do
            ::Foo::Bar; ::Foo::Bar; ::Foo::Bar; ::Foo::Bar; ::Foo::Bar; ::Foo::Bar; ::Foo::Bar; ::Foo::Bar; ::Foo::Bar; ::Foo::Bar
          end
        end
        $bm.report("1m colon2 (constant 3)") do
          1_000_000.times do
            Foo::Bar::Gar; Foo::Bar::Gar; Foo::Bar::Gar; Foo::Bar::Gar; Foo::Bar::Gar; Foo::Bar::Gar; Foo::Bar::Gar; Foo::Bar::Gar; Foo::Bar::Gar; Foo::Bar::Gar
          end
        end
        $bm.report("1m colon2 (constant 4)") do
          1_000_000.times do
            Foo::Bar::Gar::Har; Foo::Bar::Gar::Har; Foo::Bar::Gar::Har; Foo::Bar::Gar::Har; Foo::Bar::Gar::Har; Foo::Bar::Gar::Har; Foo::Bar::Gar::Har; Foo::Bar::Gar::Har; Foo::Bar::Gar::Har; Foo::Bar::Gar::Har
          end
        end
      end
      $bm.report("control, const access from Object") do
        1_000_000.times do
          Foo; Foo; Foo; Foo; Foo; Foo; Foo; Foo; Foo; Foo
        end
      end
      # FIXME: This isn't working right..
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
