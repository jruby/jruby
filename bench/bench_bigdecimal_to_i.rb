require 'benchmark/ips'
require 'bigdecimal'

# gem install benchmark-ips
# BigDecimal#to_i with and without a fractional part, at several stack
# depths. truncate returns the same Integer and is the baseline.
#   jruby bench/bench_bigdecimal_to_i.rb
# Optional: DEPTHS=0,300 WARMUP=2 TIME=5

def deep(n, &block)
  n.zero? ? block.call : deep(n - 1, &block)
end

values = {
  '12.49' => BigDecimal('12.49'),
  '12' => BigDecimal('12'),
  '-3.5' => BigDecimal('-3.5'),
  '1e20 - 0.1' => BigDecimal('99999999999999999999.9'),
  '1e20 - 1' => BigDecimal('99999999999999999999'),
}

depths = ENV.fetch('DEPTHS', '0,100,300,600').split(',').map(&:to_i)

depths.each do |depth|
  puts "\nstack depth #{depth}"
  deep(depth) do
    Benchmark.ips do |x|
      x.config(warmup: Float(ENV.fetch('WARMUP', '2')), time: Float(ENV.fetch('TIME', '5')))
      values.each do |name, value|
        x.report("to_i #{name}") do |i|
          while i > 0
            i -= 1
            value.to_i
          end
        end
      end

      fraction = values['12.49']
      x.report('truncate 12.49') do |i|
        while i > 0
          i -= 1
          fraction.truncate
        end
      end
    end
  end
end
