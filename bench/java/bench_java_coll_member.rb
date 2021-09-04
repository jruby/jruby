require 'java'
require 'benchmark'

TIMES = (ARGV[0] || 5).to_i

CONTENTS = (1..36).to_a

TIMES.times do
  Benchmark.bm(10) do |bm|
    bm.report('ArrayList#include? hit ') do
      list = java.util.ArrayList.new CONTENTS
      hit = 32
      1_000_000.times { list.include?(hit) }
    end
    bm.report('ArrayList#include? miss') do
      list = java.util.ArrayList.new CONTENTS
      miss = 0
      1_000_000.times { list.include?(miss) }
    end
    bm.report('ArrayList#contains hit ') do
      list = java.util.ArrayList.new CONTENTS
      hit = 32
      1_000_000.times { list.contains(hit) }
    end
    bm.report('ArrayList#contains miss') do
      list = java.util.ArrayList.new CONTENTS
      miss = 0
      1_000_000.times { list.contains(miss) }
    end

    bm.report('HashSet#include? hit   ') do
      list = java.util.HashSet.new CONTENTS
      hit = 32
      1_000_000.times { list.include?(hit) }
    end
    bm.report('HashSet#include? miss  ') do
      list = java.util.HashSet.new CONTENTS
      miss = 0
      1_000_000.times { list.include?(miss) }
    end
    bm.report('HashSet#contains hit   ') do
      list = java.util.HashSet.new CONTENTS
      hit = 32
      1_000_000.times { list.contains(hit) }
    end
    bm.report('HashSet#contains miss  ') do
      list = java.util.HashSet.new CONTENTS
      miss = 0
      1_000_000.times { list.contains(miss) }
    end

    bm.report('LHashSet#include? hit  ') do
      list = java.util.LinkedHashSet.new CONTENTS
      hit = 32
      1_000_000.times { list.include?(hit) }
    end
    bm.report('LHashSet#include? miss ') do
      list = java.util.LinkedHashSet.new CONTENTS
      miss = 0
      1_000_000.times { list.include?(miss) }
    end
    bm.report('LHashSet#contains hit  ') do
      list = java.util.LinkedHashSet.new CONTENTS
      hit = 32
      1_000_000.times { list.contains(hit) }
    end
    bm.report('LHashSet#contains miss ') do
      list = java.util.LinkedHashSet.new CONTENTS
      miss = 0
      1_000_000.times { list.contains(miss) }
    end
  end
end
