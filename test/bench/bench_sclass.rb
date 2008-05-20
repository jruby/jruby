require 'benchmark'

Benchmark.bmbm do |bm|
  bm.report("control, create 10 objects") do
    100_000.times do
      Object.new; Object.new; Object.new; Object.new; Object.new
      Object.new; Object.new; Object.new; Object.new; Object.new
    end
  end
  bm.report("create 10 objects and class << obj") do
    100_000.times do
      class << Object.new; end; class << Object.new; end
      class << Object.new; end; class << Object.new; end
      class << Object.new; end; class << Object.new; end
      class << Object.new; end; class << Object.new; end
      class << Object.new; end; class << Object.new; end
    end
  end
end
