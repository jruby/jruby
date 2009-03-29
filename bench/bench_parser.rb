require 'java'
require 'jruby'
require 'benchmark'
require 'rbconfig'

# benchmark 100 parses of the RDoc rb parser
ITER_COUNT = 25
filename = Config::CONFIG['rubylibdir'] + "/rdoc/parsers/parse_rb.rb"
src = File.read(filename)

puts "file: " + filename
puts "size: " + src.size.to_s

fulltime = 0

(ARGV[0] || 5).to_i.times do 
  parsetime = Benchmark.measure { ITER_COUNT.times { |i| JRuby.parse(src, "parse_rb.rb") } }.real
  
  puts "time: " + parsetime.to_s
  fulltime += parsetime
end

puts "full time: " + fulltime.to_s
puts "average: " + (fulltime / (ITER_COUNT*5)).to_s

