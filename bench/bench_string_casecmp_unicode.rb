# frozen_string_literal: true
require 'benchmark/ips'

# gem install benchmark-ips
# ruby bench/bench_string_casecmp_unicode.rb LABEL [SAVE_PREFIX]
# Optional: CASE=unicode_short WARMUP=2 TIME=5
# Saved results allow benchmark-ips to compare separate Ruby processes.
label = ARGV.fetch(0, RUBY_DESCRIPTION)
save_prefix = ARGV[1]
utf8 = Encoding::UTF_8
lower = [*"\u{e0}".."\u{ff}"].join('') * 100
upper = [*"\u{c0}".."\u{df}"].join('') * 100
changed = "ÄΣЖĞŇÅÖÜ" * 400

cases = [
  ['ascii_known', :warm, 'HelloWorldAbCd00', 'hELLOwORLDaBcD00', true],
  ['ascii_fresh', :fresh, ('ab' * 16).bytes, ('AB' * 16).bytes, true],
  ['unicode_short', :warm, 'Straße', 'STRASSE', true],
  ['unicode_fresh', :fresh, 'Straße'.bytes, 'STRASSE'.bytes, true],
  ['unicode_mismatch_6200', :warm, lower, upper, false],
  ['unicode_identical_6200', :warm, lower, lower.bytes.pack('C*').force_encoding(utf8), true],
  ['unicode_changed_6400', :warm, changed, changed.downcase(:fold), true],
  ['unicode_expansions', :warm, 'ΐßİﬃΣ' * 32, "ι\u0308\u0301ssi\u0307ffiσ" * 32, true],
]

cases.each do |name, mode, left, right, expected|
  next if ENV['CASE'] && ENV['CASE'] != name
  [left, right].each(&:ascii_only?) if mode == :warm
  sample_left, sample_right = mode == :fresh ? [left.pack('C*').force_encoding(utf8), right.pack('C*').force_encoding(utf8)] : [left, right]
  raise "incorrect comparison for #{name}" unless sample_left.casecmp?(sample_right) == expected
  puts "\n#{name}"
  Benchmark.ips do |x|
    x.config(warmup: Float(ENV.fetch('WARMUP', '2')), time: Float(ENV.fetch('TIME', '5')))
    if mode == :fresh
      # Includes creation of new strings with unknown coderanges.
      x.report(label) { left.pack('C*').force_encoding(utf8).casecmp?(right.pack('C*').force_encoding(utf8)) }
    else
      x.report(label) { left.casecmp?(right) }
    end
    x.save! "#{save_prefix}-#{name}.json" if save_prefix
    x.compare!
  end
end
