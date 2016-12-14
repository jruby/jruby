require 'java'
require 'benchmark'

TIMES = (ARGV[0] || 5).to_i

TIMES.times do
  Benchmark.bm(10) do |bm|
    bm.report('ArrayList') do
      list = java.util.ArrayList.new
      one = 1.to_java
      two = 2.to_java
      thr = 3.to_java
      1_000_000.times do
        list.clear

        list << one; list << two; list << thr

        list[2] = one; list[0] = two; list[1] = thr

        list.index(one)
        list.rindex(thr)

        list.to_a
      end
    end
    bm.report('LinkedList') do
      list = java.util.LinkedList.new
      one = 1.to_java
      two = 2.to_java
      thr = 3.to_java
      1_000_000.times do
        list.clear

        list << one; list << two; list << thr

        list[2] = one; list[0] = two; list[1] = thr

        list.index(one)
        list.rindex(thr)

        list.to_a
      end
    end
  end
end
