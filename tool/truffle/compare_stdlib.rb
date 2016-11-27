name = ARGV.first

require 'pathname'

org = Pathname("lib/ruby/stdlib/#{name}")
dst = Pathname("lib/ruby/truffle/mri/#{name}")

a = Pathname.glob(org + '**/*.rb').map { |p| p.relative_path_from org }
b = Pathname.glob(dst + '**/*.rb').map { |p| p.relative_path_from dst }

puts "Extra files:"
puts b-a

puts "Missing files:"
puts a-b
