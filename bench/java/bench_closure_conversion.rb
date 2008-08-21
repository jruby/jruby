require 'java'
require 'benchmark'

def foo(&b)
end

TIMES = (ARGV[0] || 5).to_i
Benchmark.bm(30) do |bm|
  TIMES.times {
    bm.report("pass block, to proc") do
      1_000.times {foo {}}
    end
    bm.report("block as UncaughtExHndlr") do
      thread = java.lang.Thread.currentThread
      1_000.times{thread.setUncaughtExceptionHandler {}}
    end
  }
end
