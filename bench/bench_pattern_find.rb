require 'benchmark/ips'

def call(*val)
  case val
  in [*, Integer, Hash, String] => response
    1
  in [*, Integer, String] => response
    2
  in [*, Integer => status]
    3
  in [*, String => body]
    4
  end
end

Benchmark.ips do |bm|
  bm.report("first") do |i|
    while i > 0
      i-=1
      call(nil, 201, {}, "created")
    end
  end
  bm.report("second") do |i|
    while i > 0
      i-=1
      call(nil, 200, "ok")
    end
  end
  bm.report("third") do |i|
    while i > 0
      i-=1
      call(nil, 401)
    end
  end
  bm.report("fourth") do |i|
    while i > 0
      i-=1
      call(nil, "ok")
    end
  end
end