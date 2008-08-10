require 'benchmark'

class MM
  def method_missing(sym, *args)
    1
  end
end
class MM2
  def method_missing(sym, *args)
    send :bar
  end
  
  def bar
    1
  end
end
class MM2a
  def method_missing(sym, *args)
    send :bar1, *args
  end
  
  def bar1(a)
    1
  end
end
class MM2b
  def method_missing(sym, *args)
    send :bar4, *args
  end
  
  def bar4(a,b,c,d)
    1
  end
end
class MM3
  def method_missing(sym, *args, &block)
    1
  end
end

mm = MM.new
mm2 = MM2.new
mm2a = MM2a.new
mm2b = MM2b.new
mm3 = MM3.new

5.times {
Benchmark.bm(40) do |bm|
  bm.report("1M method_missing") do
    1000000.times { mm.foo }
  end
  bm.report("1M sends") do
    1000000.times { mm2.send :bar }
  end
  bm.report("1M method_missing with send") do
    1000000.times { mm2.foo }
  end
  bm.report("1M method_missing with block") do
    1000000.times { mm3.foo }
  end
  bm.report("1M 1-arg method_missing") do
    1000000.times { mm.foo 1 }
  end
  bm.report("1M 1-arg sends") do
    1000000.times { mm2a.send :bar1, 1 }
  end
  bm.report("1M 1-arg method_missing with send") do
    1000000.times { mm2a.foo 1 }
  end
  bm.report("1M 1-arg method_missing with block") do
    1000000.times { mm3.foo 1 }
  end
  bm.report("1M 4-arg method_missing") do
    1000000.times { mm.foo 1,2,3,4 }
  end
  bm.report("1M 4-arg sends") do
    1000000.times { mm2b.send :bar,1,2,3,4 }
  end
  bm.report("1M 4-arg method_missing with send") do
    1000000.times { mm2b.foo 1,2,3,4 }
  end
  bm.report("1M 4-arg method_missing with block") do
    1000000.times { mm3.foo 1,2,3,4 }
  end
end
}
