# frozen_string_literal: true
require 'benchmark/ips'

# gem install benchmark-ips
# Compare Rubies using benchmark-ips' saved results (one file per case):
#   ruby  bench/bench_string_casemap.rb baseline /tmp/casemap
#   jruby bench/bench_string_casemap.rb candidate /tmp/casemap
# Optional: CASE=capitalize_ascii_1000 WARMUP=2 TIME=5
label = ARGV.fetch(0, RUBY_DESCRIPTION)
save_prefix = ARGV[1]

inputs = {
  'ascii_1' => 'a',
  'ascii_100' => ('hello World ' * 9)[0, 100],
  'ascii_1000' => ('hello World ' * 84)[0, 1000],
  'binary_100' => ("hello W\xC3\xB6rld " * 8).byteslice(0, 100).b,
  'utf8_100' => ("héllo Wörld " * 7) + 'ab', # 100 bytes
}

%w[upcase downcase capitalize swapcase].each do |op|
  bang = :"#{op}!"
  inputs.each do |input, str|
    name = "#{op}_#{input}"
    next if ENV['CASE'] && ENV['CASE'] != name
    puts "\n#{name}"
    Benchmark.ips do |x|
      x.config(warmup: Float(ENV.fetch('WARMUP', '2')), time: Float(ENV.fetch('TIME', '5')))
      x.report("#{label} #{op}") { str.public_send(op) }
      # Includes the dup needed to keep the input unmodified.
      x.report("#{label} #{bang}") { str.dup.public_send(bang) }
      x.save! "#{save_prefix}-#{name}.json" if save_prefix
      x.compare!
    end
  end
end
