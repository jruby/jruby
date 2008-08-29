require 'rubygems'
require 'action_controller'
require 'benchmark'

mime_type = "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5"
(ARGV[0] || 10).to_i.times {
  puts Benchmark.measure { 1000.times { Mime::Type.parse(mime_type) } }
}
