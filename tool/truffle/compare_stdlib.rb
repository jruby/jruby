name = ARGV.first

# gem install path
require 'path'

org = Path("lib/ruby/stdlib/#{name}")
dst = Path("lib/ruby/truffle/mri/#{name}")

a = org.glob("**/*.rb").map { |p| p % org }
b = dst.glob("**/*.rb").map { |p| p % dst }

puts "Extra files:"
puts b-a

puts "Missing files:"
puts a-b
