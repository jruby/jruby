require 'benchmark'

def loop_times(times = 10000000)
  i = 0
  p = proc { i += 1 }
  while i < times
    p.call
  end
end

5.times { puts Benchmark.realtime { loop_times }}