require 'benchmark'

def bench_symbol(bm)
  bm.report "1m x100 literal symbols" do
    i = 0;
    while i < 1000000
      :a; :a; :a; :a; :a; :a; :a; :a; :a; :a;
      :a; :a; :a; :a; :a; :a; :a; :a; :a; :a;
      :a; :a; :a; :a; :a; :a; :a; :a; :a; :a;
      :a; :a; :a; :a; :a; :a; :a; :a; :a; :a;
      :a; :a; :a; :a; :a; :a; :a; :a; :a; :a;
      :a; :a; :a; :a; :a; :a; :a; :a; :a; :a;
      :a; :a; :a; :a; :a; :a; :a; :a; :a; :a;
      :a; :a; :a; :a; :a; :a; :a; :a; :a; :a;
      :a; :a; :a; :a; :a; :a; :a; :a; :a; :a;
      :a; :a; :a; :a; :a; :a; :a; :a; :a; :a;
      i += 1;
    end
  end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_symbol(bm)} }
end
