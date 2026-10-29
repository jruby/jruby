# frozen_string_literal: true
require 'benchmark/ips'

# gem install benchmark-ips
# Compare Rubies using benchmark-ips' saved results (one file per case):
#   ruby  bench/bench_string_casecmp_p.rb baseline /tmp/casecmp
#   jruby bench/bench_string_casecmp_p.rb candidate /tmp/casecmp
# Optional: CASE=ascii_equal_16 WARMUP=2 TIME=5
label = ARGV.fetch(0, RUBY_DESCRIPTION)
save_prefix = ARGV[1]
utf8 = Encoding::UTF_8
binary = Encoding::ASCII_8BIT

cases = [
  ['ascii_equal_16', :warm, 'HelloWorldAbCd00', 'hELLOwORLDaBcD00'],
  ['ascii_miss_first_256', :warm, 'a' + 'b' * 255, 'z' + 'B' * 255],
  ['binary_ascii_32', :warm, ('ab' * 16).b, ('AB' * 16).b],
  ['fresh_utf8_32', :fresh, ('ab' * 16).bytes, ('AB' * 16).bytes],
  ['unicode_full_fold', :warm, "Stra\u00dfe", 'STRASSE'],
  ['cold_utf8_short', :cold, "Stra\u00dfe", 'STRASSE'.bytes.pack('C*').force_encoding(utf8)],
  ['cold_utf8_prefix_4096', :cold, 'a' * 4094 + "\u00c4", 'A' * 4094 + "\u00e4"],
  ['upstream_1_cold', :cold, [*"\u{e0}".."\u{ff}"].join(''), [*"\u{c0}".."\u{df}"].join('')],
  ['upstream_100_cold', :cold, [*"\u{e0}".."\u{ff}"].join('') * 100, [*"\u{c0}".."\u{df}"].join('') * 100],
]

cases.each do |name, mode, left, right|
  next if ENV['CASE'] && ENV['CASE'] != name
  [left, right].each(&:ascii_only?) if mode == :warm
  left, right = left.dup, right.dup if mode == :cold
  puts "\n#{name}"
  Benchmark.ips do |x|
    x.config(warmup: Float(ENV.fetch('WARMUP', '2')), time: Float(ENV.fetch('TIME', '5')))
    case mode
    when :warm
      x.report(label) { left.casecmp?(right) }
    when :fresh
      # Includes constructing new UTF-8 strings with unknown coderanges.
      x.report(label) { left.pack('C*').force_encoding(utf8).casecmp?(right.pack('C*').force_encoding(utf8)) }
    when :cold
      x.report(label) do
        # Includes metadata reset; same-encoding force_encoding is a no-op on CRuby.
        left.force_encoding(binary).force_encoding(utf8)
        right.force_encoding(binary).force_encoding(utf8)
        left.casecmp?(right)
      end
    end
    x.save! "#{save_prefix}-#{name}.json" if save_prefix
    x.compare!
  end
end
