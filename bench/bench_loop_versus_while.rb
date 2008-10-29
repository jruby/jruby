require 'benchmark'

TIMES = (ARGV[0] || 5).to_i
TIMES.times do
  Benchmark.bm(40) do |bm|
    bm.report("while true, break after 10m") do
      a = 0
      while true
        break if a == 10_000_000
        a += 1
      end
    end
    bm.report("while true in block, break after 10m") do
      a = 0
      1.times do
        while true
          break if a == 10_000_000
          a += 1
        end
      end
    end
    bm.report("loop do, break after 10m") do
      a = 0
      loop do
        break if a == 10_000_000
        a += 1
      end
    end
    bm.report("1m while true, break after 10") do
      1_000_000.times do
        a = 0
        while true
          break if a == 10
          a += 1
        end
      end
    end
    bm.report("1m while true in block, break after 10") do
      1_000_000.times do
        a = 0
        1.times do
          while true
            break if a == 10
            a += 1
          end
        end
      end
    end
    bm.report("1m loop do, break after 10") do
      1_000_000.times do
        a = 0
        loop do
          break if a == 10
          a += 1
        end
      end
    end
    bm.report("10m while true, break immediately") do
      10_000_000.times do
        while true
          break
        end
      end
    end
    bm.report("10m while true in block, break immediately") do
      10_000_000.times do
        1.times do
          while true
            break
          end
        end
      end
    end
    bm.report("10m loop do, break immediately") do
      10_000_000.times do
        loop do
          break
        end
      end
    end
  end
end