require 'benchmark'
require 'rubygems'

puts "Benchmark running repeated gem installs of rake"
puts "** I have not confirmed subsequent runs don't have some work already done..."
args = %w[ install lib/ruby/gems/1.8/cache/rake-0.7.2.gem ]
5.times {
  puts Benchmark.measure {
    Gem.manage_gems
    Gem::GemRunner.new.run(args)
  }
}
