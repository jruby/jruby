require 'benchmark/ips'

class Foo
  def foo; end
end

def respond_to_foo(obj)
  obj.respond_to?(:foo)
end

def respond_to_bar(obj)
  obj.respond_to?(:bar)
end

Benchmark.ips do |ips|
  obj = Foo.new

  ips.report("control") do |i|
    while i > 0
      i -= 1
      break unless obj && obj && obj && obj && obj && obj && obj && obj && obj && obj
    end
  end

  ips.report("10x respond_to? = true") do |i|
    while i > 0
      i -= 1
      respond_to_foo(obj)
      respond_to_foo(obj)
      respond_to_foo(obj)
      respond_to_foo(obj)
      respond_to_foo(obj)
      respond_to_foo(obj)
      respond_to_foo(obj)
      respond_to_foo(obj)
      respond_to_foo(obj)
      respond_to_foo(obj)
    end
  end

  ips.report("10x respond_to? = false") do |i|
    while i > 0
      i -= 1
      respond_to_bar(obj)
      respond_to_bar(obj)
      respond_to_bar(obj)
      respond_to_bar(obj)
      respond_to_bar(obj)
      respond_to_bar(obj)
      respond_to_bar(obj)
      respond_to_bar(obj)
      respond_to_bar(obj)
      respond_to_bar(obj)
    end
  end
end