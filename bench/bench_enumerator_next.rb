require 'benchmark'
require 'enumerator' if RUBY_VERSION =~ /1\.8/

(ARGV[0] || 1).to_i.times do
  Benchmark.bm(30) do |bm|
    bm.report "control" do
      1000.times do
        ary = Array.new(1000,0)
        a = 0
        b = 0
        while a < 1000
          b += ary[a]
          a += 1
        end
      end
    end
    bm.report "wide" do
      100.times do
        ary = Array.new(10000,0)
        enum = ary.each
        a = 0
        b = 0
        while a < 10000
          b += enum.next
          a += 1
        end
      end
    end
    bm.report "tall" do
      10000.times do
        ary = Array.new(100,0)
        enum = ary.each
        a = 0
        b = 0
        while a < 100
          b += enum.next
          a += 1
        end
      end
    end
  end
end
