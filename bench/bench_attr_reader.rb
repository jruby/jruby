require 'benchmark'

class Foo
  attr_accessor :a

  def a2
    @a
  end

  def initialize
    @a = 1
  end

  def bench(bm)
    bm.report "control: 10m attr_reader" do
      i = 0;
      while i < 100000
        a; a; a; a; a; a; a; a; a; a;
        a; a; a; a; a; a; a; a; a; a;
        a; a; a; a; a; a; a; a; a; a;
        a; a; a; a; a; a; a; a; a; a;
        a; a; a; a; a; a; a; a; a; a;
        a; a; a; a; a; a; a; a; a; a;
        a; a; a; a; a; a; a; a; a; a;
        a; a; a; a; a; a; a; a; a; a;
        a; a; a; a; a; a; a; a; a; a;
        a; a; a; a; a; a; a; a; a; a;
        i += 1;
      end
    end

    bm.report "core: 10m ruby-defined attr get" do
      i = 0;
      while i < 100000
        a2; a2; a2; a2; a2; a2; a2; a2; a2; a2;
        a2; a2; a2; a2; a2; a2; a2; a2; a2; a2;
        a2; a2; a2; a2; a2; a2; a2; a2; a2; a2;
        a2; a2; a2; a2; a2; a2; a2; a2; a2; a2;
        a2; a2; a2; a2; a2; a2; a2; a2; a2; a2;
        a2; a2; a2; a2; a2; a2; a2; a2; a2; a2;
        a2; a2; a2; a2; a2; a2; a2; a2; a2; a2;
        a2; a2; a2; a2; a2; a2; a2; a2; a2; a2;
        a2; a2; a2; a2; a2; a2; a2; a2; a2; a2;
        a2; a2; a2; a2; a2; a2; a2; a2; a2; a2;
        i += 1;
      end
    end
  end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| Foo.new.bench(bm)} }
end
