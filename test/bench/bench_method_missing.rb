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

mm = MM.new
mm2 = MM2.new

Benchmark.bm(30) do |bm|
  5.times do
    bm.report("1M method_missing") do
      1000000.times { mm.foo }
    end
  end
  5.times do
    bm.report("1M sends") do
      1000000.times { mm2.send :bar }
    end
  end
  5.times do
    bm.report("1M method_missing and send") do
      1000000.times { mm2.foo }
    end
  end
end

