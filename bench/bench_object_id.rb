require 'benchmark'

(ARGV[0] || 1).to_i.times do
  Benchmark.bm(40) do |bm|
    bm.report("1M calls to obj.object_id") do
      o = Object.new
      1_000_000.times { o.object_id }
    end

    bm.report("1M calls to Object.new.object_id") do
      1_000_000.times { Object.new.object_id }
    end

    bm.report("1M * 8 threaded calls to obj.object_id") do
      o = Object.new
      (1..8).map do
        Thread.new do
          1_000_000.times { o.object_id }
        end
      end.map(&:join)
    end

    bm.report("1M * 8 threaded calls to Object.new.object_id") do
      (1..8).map do
        Thread.new do
          1_000_000.times { Object.new.object_id }
        end
      end.map(&:join)
    end
  end
end
