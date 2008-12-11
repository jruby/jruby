require 'benchmark'

TIMES = (ARGV[0] || 5).to_i
Benchmark.bm(30) do |bm|
  TIMES.times do
    bm.report("control") { 100_000.times { Process } }
    bm.report("Process.times") { 100_000.times { Process.times } }
    bm.report("Process.times 10 threads") do
      threads = (1..10).map {Thread.new{sleep}}
      100_000.times { Process.times }
      threads.each {|t| Thread.pass until t.status == 'sleep'; t.wakeup; t.join}
    end
  end
end

