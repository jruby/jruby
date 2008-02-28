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
class MM3
  def method_missing(sym, *args, &block)
    1
  end
end

mm = MM.new
mm2 = MM2.new
mm3 = MM3.new

5.times {
Benchmark.bm(30) do |bm|
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
end
}
