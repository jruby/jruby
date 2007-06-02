require 'java'
require 'jruby'
require 'benchmark'
require 'rbconfig'

# benchmark 100 parses of the RDoc rb parser
ITER_COUNT = 100
filename = Config::CONFIG['rubylibdir'] + "/rdoc/parsers/parse_rb.rb"
src = File.read(filename)

parsetime = Benchmark.measure { ITER_COUNT.times { JRuby.parse(src, "parse_rb.rb") } }.real

puts "file: " + filename
puts "size: " + src.size.to_s
puts "time: " + parsetime.to_s
puts "average: " + (parsetime / ITER_COUNT).to_s