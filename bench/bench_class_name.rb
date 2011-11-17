require 'benchmark'

cls = Class.new
nested = '
class Foo
  class Bar
    class Baz
      class Quux
        class Blah
          class Yum
            class Widget
              class Bumble
end;end;end;end;end;end;end;end'
eval nested
cls2 = Foo::Bar::Baz::Quux::Blah::Yum::Widget::Bumble
cls.class_eval nested
cls3 = cls::Foo::Bar::Baz::Quux::Blah::Yum::Widget::Bumble

(ARGV[0] || 5).to_i.times do
  Benchmark.bm(30) do |bm|
    bm.report("loop alone") do
      10000000.times { cls }
    end
    bm.report("anonymous class") do
      10000000.times { cls.name }
    end
    bm.report("deep-nested class") do
      10000000.times { cls2.name }
    end
    bm.report("deep-nested rooted at anon") do
      10000000.times { cls3.name }
    end
  end
end
