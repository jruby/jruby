require 'benchmark'
require 'digest'

puts "pass -ropenssl to use BouncyCastle digest impls"

Thread.abort_on_exception = true

(ARGV[0] || 5).to_i.times do
  Benchmark.bm(30) do |bm|
    bm.report("uncontended 1m MD5 'foo'") do
      dig = Digest::MD5
      1_000_000.times { dig.hexdigest('foo') }
    end

    bm.report("uncontended 100k MD5 'foo' * 1000") do
      dig = Digest::MD5
      str = 'foo' * 1000
      100_000.times { dig.hexdigest(str) }
    end

    bm.report("10x100k contended MD5 'foo'") do
      dig = Digest::MD5
      (1..10).map { Thread.new { 100_000.times { dig.hexdigest('foo') } } }.map(&:join)
    end

    bm.report("10x10k contended MD5 'foo' * 1000") do
      dig = Digest::MD5
      str = 'foo' * 1000
      (1..10).map { Thread.new { 10_000.times { dig.hexdigest(str) } } }.map(&:join)
    end
  end
end
