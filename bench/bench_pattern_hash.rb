require 'benchmark/ips'

def call(**val)
  case val
  in {a:1} => response
    1
  in {b:1} => response
    2
  in {c:1} => response
    3
  in {d:1} => response
    4
  end
end

Benchmark.ips do |bm|
  bm.report("first") do |i|
    while i > 0
      i-=1
      call(a:1)
    end
  end
  bm.report("second") do |i|
    while i > 0
      i-=1
      call(b:1)
    end
  end
  bm.report("third") do |i|
    while i > 0
      i-=1
      call(c:1)
    end
  end
  bm.report("fourth") do |i|
    while i > 0
      i-=1
      call(d:1)
    end
  end
end