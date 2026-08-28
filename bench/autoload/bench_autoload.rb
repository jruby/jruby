require 'benchmark'
$:.unshift(File.expand_path(".", File.dirname(__FILE__)))

5.times do
  puts Benchmark.measure {
    1000000.times do
      autoload :Foo, "foo"
      Foo::Baz::Y
    end
  }
  GC.start
end
