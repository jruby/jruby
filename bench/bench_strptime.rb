require 'benchmark'
require 'date'

10.times() do
  puts(Benchmark.realtime() do
    for i in (1..10000)
      DateTime.strptime("27/Nov/2007:15:01:43 -0800", "%d/%b/%Y:%H:%M:%S %z")
    end
  end)
end
