# Copied with permission from Antoni Cangiano's blog:
# http://antoniocangiano.com/2008/03/25/inject-each-and-times-methods-much-slower-in-ruby-19
require 'benchmark'
include Benchmark

class Integer
  def sum_with_inject
    (1..self).inject(0) { |sum, i| sum + i }
  end

  def sum_with_each
    sum = 0
    (1..self).each { |i| sum += i }
    sum
  end

  def sum_with_times
    sum = 0
    (self+1).times { |i| sum += i}
    sum
  end

  def sum_with_while
    sum, i = 0, 1
    while i <= self
      sum += i
      i += 1
    end
    sum
  end
end

(1..8).each do |p|
  n = 10**p
  puts "=== 10^#{p} ==="
  benchmark do |x|
    x.report("inject: ") { n.sum_with_inject }
    x.report("each:   ") { n.sum_with_each }
    x.report("times:  ") { n.sum_with_times }
    x.report("while:  ") { n.sum_with_while }
  end
end

