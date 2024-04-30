require 'benchmark/ips'

def call(*val)
  case val
  in [String => body]
    1
  in [Integer => status]
    2
  in [Integer, String] => response
    3
  in [Integer, Hash, String] => response
    4
  end
end

Benchmark.ips do |bm|
  bm.report("first") do |i|
    while i > 0
      i-=1
      call("ok")
    end
  end
  bm.report("second") do |i|
    while i > 0
      i-=1
      call(401)
    end
  end
  bm.report("third") do |i|
    while i > 0
      i-=1
      call(200, "ok")
    end
  end
  bm.report("fourth") do |i|
    while i > 0
      i-=1
      call(201, {}, "created")
    end
  end
end