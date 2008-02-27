require 'benchmark'

def foo1; 1; end
def foo2; return 1; end

def bench_method_return(bm)
bm.report "100k x100 implicit return" do
  a = 5; 
  i = 0;
  while i < 100000
    foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1
    foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1
    foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1
    foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1
    foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1
    foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1
    foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1
    foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1
    foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1
    foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1; foo1
    i += 1;
  end
  end

bm.report "100k x100 explicit return" do
  a = []; 
  i = 0;
  while i < 100000
    foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2
    foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2
    foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2
    foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2
    foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2
    foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2
    foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2
    foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2
    foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2
    foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2
    i += 1;
  end
end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(50) {|bm| bench_method_return(bm)} }
end