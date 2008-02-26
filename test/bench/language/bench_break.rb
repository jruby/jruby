require 'benchmark'

(ARGV[0] || 10).to_i.times do
  Benchmark.bm(30) do |bm|
    bm.report("control, five short while loops") do
      100_000.times do
        a = true; while a; a = false; end
        a = true; while a; a = false; end
        a = true; while a; a = false; end
        a = true; while a; a = false; end
        a = true; while a; a = false; end
      end
    end
    
    bm.report("control, five whiles with blocks") do
      100_000.times do
        a = true; while a; 1.times {a = false}; end
        a = true; while a; 1.times {a = false}; end
        a = true; while a; 1.times {a = false}; end
        a = true; while a; 1.times {a = false}; end
        a = true; while a; 1.times {a = false}; end
      end
    end
    
    bm.report("five whiles that break") do
      100_000.times do
        a = true; while a; a = false; break; end
        a = true; while a; a = false; break; end
        a = true; while a; a = false; break; end
        a = true; while a; a = false; break; end
        a = true; while a; a = false; break; end
      end
    end
    
    bm.report("control, five whiles with blocks") do
      100_000.times do
        a = true; while a; 1.times {a = false; break}; end
        a = true; while a; 1.times {a = false; break}; end
        a = true; while a; 1.times {a = false; break}; end
        a = true; while a; 1.times {a = false; break}; end
        a = true; while a; 1.times {a = false; break}; end
      end
    end
  end
end