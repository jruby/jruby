require 'benchmark/ips'
require 'digest'

if defined?(OpenSSL)
  puts "using OpenSSL, BouncyCastle digest impls"
else
  puts "pass -ropenssl to use BouncyCastle digest impls"
end

Thread.abort_on_exception = true

Benchmark.ips do |bm|
  bm.report("uncontended MD5 'foo'") do |n|
    dig = Digest::MD5
    n.times { dig.hexdigest('foo') }
  end

  bm.report("uncontended MD5 'foo' * 1000") do |n|
    dig = Digest::MD5
    str = 'foo' * 1000
    n.times { dig.hexdigest(str) }
  end

  bm.report("4x contended MD5 'foo'") do |n|
    dig = Digest::MD5
    (1..4).map { Thread.new { n.times { dig.hexdigest('foo') } } }.map(&:join)
  end

  bm.report("4x contended MD5 'foo' * 1000") do |n|
    dig = Digest::MD5
    str = 'foo' * 1000
    (1..4).map { Thread.new { n.times { dig.hexdigest(str) } } }.map(&:join)
  end

  bm.report("10x contended MD5 'foo'") do |n|
    dig = Digest::MD5
    (1..10).map { Thread.new { n.times { dig.hexdigest('foo') } } }.map(&:join)
  end

  bm.report("10x contended MD5 'foo' * 1000") do |n|
    dig = Digest::MD5
    str = 'foo' * 1000
    (1..10).map { Thread.new { n.times { dig.hexdigest(str) } } }.map(&:join)
  end

  bm.report("100x contended MD5 'foo'") do |n|
    dig = Digest::MD5
    (1..100).map { Thread.new { n.times { dig.hexdigest('foo') } } }.map(&:join)
  end

  bm.report("100x contended MD5 'foo' * 1000") do |n|
    dig = Digest::MD5
    str = 'foo' * 1000
    (1..100).map { Thread.new { n.times { dig.hexdigest(str) } } }.map(&:join)
  end
end
