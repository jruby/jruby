require 'benchmark'

TIMES = 10_000_000

Benchmark.bmbm do |x|

  x.report("'foo'.index('o') [#{TIMES}x]") do
    TIMES.times do
      'foo'.index('o')
    end
  end

  x.report("'0123456789001234567890'.index('abc') [#{TIMES}x]") do
    TIMES.times do
      '0123456789001234567890'.index('abc')
    end
  end

  x.report("(('foo' * 100) + '012').index('01') [#{TIMES}x]") do
    str = (('foo' * 100) + '012')
    TIMES.times do
      str.index('01')
    end
  end

end
