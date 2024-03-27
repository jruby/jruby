require 'benchmark/ips'

def foo
  block_given?
end

def foo2
  defined?(yield)
end

Benchmark.ips do |bm|
  bm.report("block") do |i|
    while i > 0
      i-=1
      foo { }; foo { }; foo { }; foo { }; foo { }; foo { }; foo { }; foo { }; foo { }; foo { }
    end
  end

  bm.report("no block") do |i|
    while i > 0
      i-=1
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo
    end
  end

  bm.report("defined block") do |i|
    while i > 0
      i-=1
      foo2 { }; foo2 { }; foo2 { }; foo2 { }; foo2 { }; foo2 { }; foo2 { }; foo2 { }; foo2 { }; foo2 { }
    end
  end

  bm.report("defined no block") do |i|
    while i > 0
      i-=1
      foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2; foo2
    end
  end
end