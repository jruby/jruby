require 'benchmark'

TIMES = (ENV['TIMES'] || 5_000_000).to_i

Benchmark.bmbm do |x|

  ary1 = [1, 2, 3, 4, 5, 4, 3, 3]
  x.report("Array#uniq (size: #{ary1.length}) [#{TIMES}x]") do
    TIMES.times do
      ary1.uniq
    end
  end

  ary2 = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16]
  ary2.push 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16
  x.report("Array#uniq (size: #{ary2.length}) [#{TIMES}x]") do
    TIMES.times do
      ary2.uniq
    end
  end

  ary3 = [1, 2, 3, -1, -2, -3, 3, -3]
  x.report("Array#uniq(&block) (size: #{ary3.length}) [#{TIMES}x]") do
    TIMES.times do
      ary3.uniq { |e| e.abs }
    end
  end

  enum1 = ary1.to_enum
  x.report("Enumerable#uniq (size: #{ary1.length}) [#{TIMES}x]") do
    TIMES.times do
      enum1.uniq
    end
  end

  enum2 = ary2.to_enum
  x.report("Enumerable#uniq (size: #{ary2.length}) [#{TIMES}x]") do
    TIMES.times do
      enum2.uniq
    end
  end

  enum3 = ary3.to_enum
  x.report("Enumerable#uniq(&block) (size: #{ary3.length}) [#{TIMES}x]") do
    TIMES.times do
      enum3.uniq { |e| e.abs }
    end
  end

end