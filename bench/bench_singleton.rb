require 'benchmark/ips'

loop {
Benchmark.ips do |bm|
  bm.warmup = 10
  bm.report("1000 class << Object.new") {
    i = 0
    while i < 1000
      i+=1
      class << Object.new; end
    end
  }
end
}