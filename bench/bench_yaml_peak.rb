require 'psych'
require 'benchmark'
require 'benchmark/ips'

file_name = "YAML.txt"

puts Benchmark.measure {Psych.load(File.read(file_name))}

Benchmark.ips do |x|
  x.warmup = 5
  x.iterations = 3
  x.report('Psych.load') { Psych.load(File.read(file_name))}
end

puts Benchmark.measure {Psych.load(File.read(file_name))}
