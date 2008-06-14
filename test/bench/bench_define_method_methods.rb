require 'benchmark'

class Object
  def quux; 1; end
  define_method(:foo) {1}
  define_method(:bar) {a = 1}
  b = 1
  define_method(:baz) {b = 2}
  eval "define_method(:boo) {1}"
end

def bench_define_method_methods(bm)
  bm.report "control, simple method, 10k * 100 invocations" do
    a = 0
    while a < 10000
      quux; quux; quux; quux; quux; quux; quux; quux; quux; quux
      quux; quux; quux; quux; quux; quux; quux; quux; quux; quux
      quux; quux; quux; quux; quux; quux; quux; quux; quux; quux
      quux; quux; quux; quux; quux; quux; quux; quux; quux; quux
      quux; quux; quux; quux; quux; quux; quux; quux; quux; quux
      quux; quux; quux; quux; quux; quux; quux; quux; quux; quux
      quux; quux; quux; quux; quux; quux; quux; quux; quux; quux
      quux; quux; quux; quux; quux; quux; quux; quux; quux; quux
      quux; quux; quux; quux; quux; quux; quux; quux; quux; quux
      quux; quux; quux; quux; quux; quux; quux; quux; quux; quux
      a += 1
    end
  end

  bm.report "define_method(:foo) {1}, 10k * 100 invocations" do
    a = 0
    while a < 10000
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo
      a += 1
    end
  end

  bm.report "eval'ed define_method(:baz) {1}, 10k * 100 invocations" do
    a = 0
    while a < 10000
      boo; boo; boo; boo; boo; boo; boo; boo; boo; boo
      boo; boo; boo; boo; boo; boo; boo; boo; boo; boo
      boo; boo; boo; boo; boo; boo; boo; boo; boo; boo
      boo; boo; boo; boo; boo; boo; boo; boo; boo; boo
      boo; boo; boo; boo; boo; boo; boo; boo; boo; boo
      boo; boo; boo; boo; boo; boo; boo; boo; boo; boo
      boo; boo; boo; boo; boo; boo; boo; boo; boo; boo
      boo; boo; boo; boo; boo; boo; boo; boo; boo; boo
      boo; boo; boo; boo; boo; boo; boo; boo; boo; boo
      boo; boo; boo; boo; boo; boo; boo; boo; boo; boo
      a += 1
    end
  end

  bm.report "define_method(:bar) {a = 1}, 10k * 100 invocations" do
    a = 0
    while a < 10000
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      a += 1
    end
  end

  bm.report "b = 1; define_method(:baz) {b = 2}, 10k * 100 invocations" do
    a = 0
    while a < 10000
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz
      a += 1
    end
  end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(60) {|bm| bench_define_method_methods(bm) } }
end
