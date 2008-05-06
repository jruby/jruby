require 'test/minirunit'
test_check "Test Struct"

s = Struct.new("MyStruct", :x, :y)

test_equal(Struct::MyStruct, s)

s1 = s.new(11, 22)
test_equal(11, s1["x"])
test_equal(22, s1["y"])

s1 = s.new
test_equal(nil, s1.x)
test_equal(nil, s1.y)
s1.x = 10
s1.y = 20
test_equal(10, s1.x)
test_equal(20, s1.y)
test_equal(10, s1[0])
test_equal(20, s1[1])
test_equal(20, s1[-1])

test_equal(2, s1.length)
test_equal(2, s1.size)

test_equal([10, 20], s1.values)

test_exception(NameError) { s1["froboz"] }
test_exception(IndexError) { s1[10] }

s2 = s.new(1, 2)
test_equal(1, s2.x)
test_equal(2, s2.y)

test_exception(ArgumentError) { s.new(1,2,3,4,5,6,7) }
test_exception(ArgumentError) { Struct.new }
test_exception(NameError) { Struct.new('foo', 'bar') }

# name should be coerced to string with to_str
foo = Object.new
def foo.to_str() 'Foo' end
s = Struct.new(foo, :x, :y)
test_equal s, Struct::Foo

# Anonymous Struct
a = Struct.new(:x, :y)
a1 = a.new(5, 7)
test_equal(5, a1.x)

# Struct::Tms
tms = Struct::Tms.new(0, 0, 0, 0)
test_ok(tms != nil)

# Struct creation with a block
a = Struct.new(:foo, :bar) {
  def hello
    "hello"
  end
}

test_equal("hello", a.new(0, 0).hello)

require 'stringio'

module Recording
  def self.stderr
    $stderr = recorder = StringIO.new
    begin
      yield
    ensure
      $stderr = STDERR
    end
    recorder.rewind
    recorder
  end
end

# Redefining a named struct should produce a warning, but it should be a new class
P1 = Struct.new("Post", :foo)
P1.class_eval do
  def bar
    true
  end
end
Recording::stderr do
  P2 = Struct.new("Post", :foo)
end

test_exception {
  P2.new.bar
}

Recording::stderr do
  MyStruct = Struct.new("MyStruct", :a, :b)
end
class MySubStruct < MyStruct
  def initialize(v, *args) super(*args); @v = v; end 
end

b = MySubStruct.new(1, 2)
inspect1 = b.inspect
b.instance_eval {"EH"}
# Instance_eval creates a metaclass and our inspect should not print that new metaclass out
test_equal(inspect1, b.inspect)
c = MySubStruct.new(1, 2)

class << b
  def foo
  end
end

# Even though they have different metaclasses they are still equal in the eyes of Ruby
test_equal(b, c)

c = Struct.new(:a)
s1 = c.new(1)
s2 = c.new(1)
test_ok(true, s1.eql?(s2))
test_equal(s1.hash, s2.hash)

test_no_exception { Struct.new(:icandup).new(1).dup }

FiveElementStruct = Struct.new(:a, :b, :c, :d, :e)
fes = FiveElementStruct.new(1, 2, 3, 4, 5)
test_equal([2,4], fes.select {|i| (i % 2).zero?})

# JRUBY-2157
class Foo < Struct.new(:heh)
  def initialize
  end
end

test_equal(nil, Foo.new.heh)


# JRUBY-2490
require 'java'

class JavaComparableStruct < Struct.new(:foo)
  include java.lang.Comparable
  
  def compare_to(other); 0; end
end

test_equal(:a, JavaComparableStruct.new(:a).foo)