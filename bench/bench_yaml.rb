
require 'benchmark'
require 'yaml'

def bench_yaml(bm)
  oldbm = $bm
  $bm = bm

  str = File.read(File.join(File.dirname(__FILE__), "big_yaml.yml"))
  str_small = File.read(File.join(File.dirname(__FILE__), "small_yaml.yml"))
  result = YAML.load(str)
  result_small = YAML.load(str_small)
  
  $bm.report("1k small parse") { 1_000.times { YAML.parse(str_small) } }
  $bm.report("1k small loads") { 1_000.times { YAML.load(str_small) } }
  $bm.report("1k small dumps") { 1_000.times { result_small.to_yaml } }
  $bm.report("1k small roundtrip") { 1_000.times { YAML.load(result_small.to_yaml) } }

  $bm.report("1k big parse") { 1_000.times { YAML.parse(str) } }
  $bm.report("1k big loads") { 1_000.times { YAML.load(str) } }
  $bm.report("1k big dumps") { 1_000.times { result.to_yaml } }
  $bm.report("1k big roundtrip") { 1_000.times { YAML.load(result.to_yaml) } }
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(30) {|bm| bench_yaml(bm)} }
end
