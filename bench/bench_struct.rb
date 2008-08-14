require 'benchmark'

class Foo < Struct.new(:a, :b, :c, :d, :e)
end

def bench_struct(bm)
  foo = Foo.new(1, 2, 3, 4, 5)
  bm.report("struct member access") {
    1_000_000.times { foo.a; foo.b; foo.c; foo.d; foo.e }
  }
  bm.report("struct member mutate") {
    1_000_000.times { foo.a=1; foo.b=1; foo.c=1; foo.d=1; foo.e=1 }
  }
  bm.report("struct to_s") {
    1_000_000.times { foo.to_s }
  }
  bm.report("struct each") {
    1_000_000.times { foo.each {|x| x} }
  }
end

if $0 == __FILE__
  Benchmark.bmbm {|bm| bench_struct(bm)}
end
