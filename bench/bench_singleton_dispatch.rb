require 'benchmark'

Deep = Class.new(Class.new(Class.new(Class.new(Class.new(Class.new(Object))))))

(ARGV[0] || 5).to_i.times {
  Benchmark.bm(30) do |bm|
    bm.report("singletonize obj") { 100000.times { o = Object.new; class << o; end }}
    bm.report("dispatch singleton obj") { 100000.times { o = Object.new; class << o; end; o.class }}
    bm.report("dispatch singleton deep") { 100000.times { o = Deep.new; class << o; end; o.class }}
  end
}