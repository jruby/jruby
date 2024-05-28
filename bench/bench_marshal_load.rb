require 'benchmark/ips'

ARRAY_OF_FIXNUMS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
ARRAY_OF_FIXNUMS_DUMPED = Marshal.dump(ARRAY_OF_FIXNUMS)
ARRAY_OF_BIG_FIXNUMS_DUMPED = Marshal.dump([1<<30,1<<30,1<<30,1<<30,1<<30,1<<30,1<<30,1<<30,1<<30,1<<30])
ARRAY_OF_FLOATS = [1.1111111111, 2.2222222222, 3.3333333333, 4.4444444444, 5.5555555555, 6.6666666666, 7.7777777777, 8.8888888888, 9.9999999999, 10.111111111]
ARRAY_OF_FLOATS_DUMPED = Marshal.dump(ARRAY_OF_FLOATS)
ARRAY_OF_STRINGS_DUMPED = Marshal.dump(ARRAY_OF_FLOATS.map(&:to_s))
ARRAY_OF_ARRAYS_DUMPED = Marshal.dump(ARRAY_OF_FIXNUMS.map {[it]})
ARRAY_OF_HASHES_DUMPED = Marshal.dump(ARRAY_OF_FIXNUMS.map {{it => it}})

class Foo
  def initialize(val)
    @val = val
  end
end

ARRAY_OF_OBJECTS_DUMPED = Marshal.dump(ARRAY_OF_FIXNUMS.map {Foo.new(it)})
ARRAY_OF_TIMES_DUMPED = Marshal.dump(10.times.map {Time.now})

Benchmark.ips do |bm|
  bm.report("array of fixnums") do |i|
    while i > 0
      i-=1
      Marshal.load(ARRAY_OF_FIXNUMS_DUMPED)
    end
  end
  bm.report("array of big fixnums") do |i|
    while i > 0
      i-=1
      Marshal.load(ARRAY_OF_BIG_FIXNUMS_DUMPED)
    end
  end
  bm.report("array of floats") do |i|
    while i > 0
      i-=1
      Marshal.load(ARRAY_OF_FLOATS_DUMPED)
    end
  end
  bm.report("array of strings") do |i|
    while i > 0
      i-=1
      Marshal.load(ARRAY_OF_STRINGS_DUMPED)
    end
  end
  bm.report("array of arrays") do |i|
    while i > 0
      i-=1
      Marshal.load(ARRAY_OF_ARRAYS_DUMPED)
    end
  end
  bm.report("array of hashes") do |i|
    while i > 0
      i-=1
      Marshal.load(ARRAY_OF_HASHES_DUMPED)
    end
  end
  bm.report("array of objects") do |i|
    while i > 0
      i-=1
      Marshal.load(ARRAY_OF_OBJECTS_DUMPED)
    end
  end
  bm.report("array of times") do |i|
    while i > 0
      i-=1
      Marshal.dump(ARRAY_OF_TIMES_DUMPED)
    end
  end
end
