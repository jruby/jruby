require 'test/unit'
require 'benchmark'

class A < Test::Unit::TestCase
  [10_000, 100_000].each do |n|
    define_method "test_#{n}" do
      puts "test_#{n}"
      5.times do 
        puts Benchmark.measure{n.times{assert_equal true,true}}
      end
    end
  end
end
