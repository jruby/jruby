require 'benchmark'

COUNT = 10_000_000
COUNT_STR = "10M"

def bench_upto(bm)
  puts "Integer#upto -----------------"
  bm.report("#{COUNT_STR} Integer#upto, arg block") do
    1.upto(COUNT) { |i| i }
  end

  bm.report("#{COUNT_STR} Integer#upto, arg block and add") do
    1.upto(COUNT) { |i| i + 1 }
  end

  bm.report("#{COUNT_STR} Integer#upto, no-arg block") do
    1.upto(COUNT) { 0 }
  end

  bm.report("#{COUNT_STR} Integer#upto, no-arg block and add") do
    i= 0
    1.upto(COUNT) { i += 1 }
  end

  puts "Integer#downto -----------------"
  bm.report("#{COUNT_STR} Integer#downto, arg block") do
    COUNT.downto(1) { |i| i }
  end

  bm.report("#{COUNT_STR} Integer#downto, arg block and add") do
    COUNT.downto(1) { |i| i + 1 }
  end

  bm.report("#{COUNT_STR} Integer#downto, no-arg block") do
    COUNT.downto(1) { 0 }
  end

  bm.report("#{COUNT_STR} Integer#downto, no-arg block and add") do
    i= 0
    COUNT.downto(1) { i += 1 }
  end

  puts "Integer#times -----------------"
  bm.report("#{COUNT_STR} Integer#times, arg block") do
    COUNT.times { |i| i }
  end

  bm.report("#{COUNT_STR} Integer#times, arg block and add") do
    COUNT.times { |i| i + 1 }
  end

  bm.report("#{COUNT_STR} Integer#times, no-arg block") do
    COUNT.times { 0 }
  end

  bm.report("#{COUNT_STR} Integer#times, no-arg block and add") do
    i= 0
    COUNT.times { i += 1 }
  end
end

if $0 == __FILE__
  (ARGV[0] || 5).to_i.times { |iter|
    puts "============= Iteration #{iter + 1} ============= "
    Benchmark.bm(40) {|bm|
      bench_upto(bm)
    }
  }
end
