require 'benchmark'

def bench_rescue(bm)
  bm.report("control") { 1_000_000.times { foo1 } }
  bm.report("one rescue") { 1_000_000.times { foo2a } }
  bm.report("one rescue + raise") { 1_000_000.times { foo2b } }
  bm.report("five rescues") { 1_000_000.times { foo3a } }
  bm.report("five rescues + raise") { 1_000_000.times { foo3b } }
end

def foo1; end
def foo2a; rescue RuntimeError; end
def foo2b; raise; rescue RuntimeError; end
def foo3a; rescue NameError; rescue NameError; rescue NameError; rescue NameError; rescue RuntimeError; end
def foo3b; raise; rescue NameError; rescue NameError; rescue NameError; rescue NameError; rescue RuntimeError; end

if $0 == __FILE__
  TIMES = (ARGV[0] || 1).to_i
  TIMES.times {
    Benchmark.bm(40) {|bm| bench_rescue(bm)}
  }
end
