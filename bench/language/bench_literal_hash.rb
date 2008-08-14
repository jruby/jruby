require 'benchmark'

def bench_literal_hash(bm)
    bm.report("loop alone") {
      a = 1; while a < 100_000; a += 1; end
    }

    bm.report("no-element hash") {
      a = 1; while a < 100_000; {};{};{};{};{};{};{};{};{};{}; a += 1; end
    }

    bm.report("1-element hash") {
      x = 1
      a = 1; while a < 100_000; {x=>x}; {x=>x}; {x=>x}; {x=>x}; {x=>x}; {x=>x}; {x=>x}; {x=>x}; {x=>x}; {x=>x}; a += 1; end
    }

    bm.report("2-element hash") {
      x = 1
      y = 2
      a = 1; while a < 100_000; {x=>x,y=>y};{x=>x,y=>y};{x=>x,y=>y};{x=>x,y=>y};{x=>x,y=>y};{x=>x,y=>y};{x=>x,y=>y};{x=>x,y=>y};{x=>x,y=>y};{x=>x,y=>y}; a += 1; end
    }

    bm.report("3-element hash") {
      x = 1
      y = 2
      z = 3
      a = 1; while a < 100_000; {x=>x,y=>y,z=>z};{x=>x,y=>y,z=>z};{x=>x,y=>y,z=>z};{x=>x,y=>y,z=>z};{x=>x,y=>y,z=>z};{x=>x,y=>y,z=>z};{x=>x,y=>y,z=>z};{x=>x,y=>y,z=>z};{x=>x,y=>y,z=>z};{x=>x,y=>y,z=>z}; a += 1; end
    }

    bm.report("4-element hash") {
      x = 1
      y = 2
      z = 3
      w = 4
      a = 1; while a < 100_000; {x=>x,y=>y,z=>z,w=>w};{x=>x,y=>y,z=>z,w=>w};{x=>x,y=>y,z=>z,w=>w};{x=>x,y=>y,z=>z,w=>w};{x=>x,y=>y,z=>z,w=>w};{x=>x,y=>y,z=>z,w=>w};{x=>x,y=>y,z=>z,w=>w};{x=>x,y=>y,z=>z,w=>w};{x=>x,y=>y,z=>z,w=>w};{x=>x,y=>y,z=>z,w=>w}; a += 1; end
    }
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_literal_hash(bm)} }
end
