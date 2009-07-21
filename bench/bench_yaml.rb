
require 'benchmark'
require 'yaml'

def bench_yaml(bm)
  oldbm = $bm
  $bm = bm

  str = File.read(File.join(File.dirname(__FILE__), "big_yaml.yml"))
  result = YAML.load(str)
  
  $bm.report("1k parse") { 1_000.times { YAML.parse(str) } }
  $bm.report("1k loads") { 1_000.times { YAML.load(str) } }
  $bm.report("1k dumps") { 1_000.times { result.to_yaml } }
  $bm.report("1k roundtrip") { 1_000.times { YAML.load(result.to_yaml) } }
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(30) {|bm| bench_yaml(bm)} }
end
