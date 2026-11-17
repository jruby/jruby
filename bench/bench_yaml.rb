require 'psych'
require 'benchmark'

file_name = "YAML.txt"

puts Benchmark.measure {
  Psych.load(File.read(file_name))
}
