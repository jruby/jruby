require 'java'
require 'benchmark'

N = (ARGV[0] || 2 ** 21).to_i # default ~ 2_000_000

EMPTY   = java.util.Optional.empty
PRESENT = java.util.Optional.of('x')
PRESENT_INT  = java.util.OptionalInt.of(11)
EMPTY_DOUBLE = java.util.OptionalDouble.empty

LIST = java.util.ArrayList.new
LIST.add(java.lang.Integer.new(0))

EMPTY_MAP = java.util.HashMap.new

puts "----- single-threaded -----"
Benchmark.bmbm(36) do |bm|
  bm.report("Supplier       Optional#orElseGet")       { N.times { EMPTY.orElseGet { 'y' } } }
  bm.report("Function       Optional#map")             { N.times { PRESENT.map { |s| s } } }
  bm.report("Predicate      Optional#filter")          { N.times { PRESENT.filter { |_| true } } }
  bm.report("IntConsumer    OptionalInt#ifPresent")    { N.times { PRESENT_INT.ifPresent { |i| } } }
  bm.report("DoubleSupplier OptionalDouble#orElseGet") { N.times { EMPTY_DOUBLE.orElseGet { 0.0 } } }
  bm.report("BiFunction     Map#compute")              { N.times { EMPTY_MAP.compute(:key) { |k, v| nil } } }
  bm.report("BiFunction     Map#computeIfPresent")     { N.times { EMPTY_MAP.computeIfPresent(1) { fail('never called') } } }
  bm.report("ToLongFunction Comparator.comparingLong") { N.times { java.util.Comparator.comparingLong { |s| s.length } } }
  bm.report("Comparator     ArrayList#sort")           { N.times { LIST.sort { |a, b| a <=> b } } }
end

puts
puts "----- multi-threaded (4 submitter threads, N tasks each) -----"
Benchmark.bmbm(36) do |bm|
  bm.report("Consumer    Optional#ifPresent (4 threads)") do
    4.times.map { Thread.new { (N / 4).times { PRESENT.ifPresent { |_| } } } }.each(&:join)
  end
  bm.report("Consumer    Optional#ifPresent (16 threads)") do
    16.times.map { Thread.new { (N / 16).times { PRESENT.ifPresent { |_| } } } }.each(&:join)
  end
  bm.report("Consumer    Optional#ifPresent (128 threads)") do
    128.times.map { Thread.new { (N / 128).times { PRESENT.ifPresent { |_| } } } }.each(&:join)
  end

  bm.report("IntConsumer  OptionalInt#ifPresent (4 threads)") do
    4.times.map { Thread.new { (N / 4).times { PRESENT_INT.ifPresent { |i| } } } }.each(&:join)
  end
  bm.report("IntConsumer  OptionalInt#ifPresent (128 threads)") do
    128.times.map { Thread.new { (N / 128).times { PRESENT_INT.ifPresent { |i| } } } }.each(&:join)
  end
  bm.report("IntConsumer  OptionalInt#ifPresent (512 threads)") do
    512.times.map { Thread.new { (N / 512).times { PRESENT_INT.ifPresent { |i| } } } }.each(&:join)
  end

  bm.report("Comparator  List#stream.sorted (16 threads)") do
    16.times.map { Thread.new { (N / 16).times { LIST.stream.sorted { |a, b| a <=> b } } } }.each(&:join)
  end
  bm.report("Comparator  List#stream.sorted (128 threads)") do
    128.times.map { Thread.new { (N / 128).times { LIST.stream.sorted { |a, b| a <=> b } } } }.each(&:join)
  end
  bm.report("Comparator  List#stream.sorted (512 threads)") do
    512.times.map { Thread.new { (N / 512).times { LIST.stream.sorted { |a, b| a <=> b } } } }.each(&:join)
  end
end
