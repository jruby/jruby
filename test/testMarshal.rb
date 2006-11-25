require 'test/minirunit'
test_check "Test Marshal:"

MARSHAL_HEADER = Marshal.dump(nil).chop

def test_marshal(expected, marshalee)
  test_equal(MARSHAL_HEADER + expected, Marshal.dump(marshalee))
end

test_marshal("0", nil)
test_marshal("T", true)
test_marshal("F", false)
test_marshal("i\000", 0)
test_marshal("i\006", 1)
test_marshal("i\372", -1)
test_marshal("i\002\320\a", 2000)
test_marshal("i\3760\370", -2000)
test_marshal("i\004\000\312\232;", 1000000000)
test_marshal(":\017somesymbol", :somesymbol)
test_marshal("f\n2.002", 2.002)
test_marshal("f\013-2.002", -2.002)
test_marshal("\"\nhello", "hello")
test_marshal("[\010i\006i\ai\010", [1,2,3])
test_marshal("{\006i\006i\a", {1=>2})
test_marshal("c\013Object", Object)
module Foo
  class Bar
  end
end
test_marshal("c\rFoo::Bar", Foo::Bar)
test_marshal("m\017Enumerable", Enumerable)
test_marshal("/\013regexp\000", /regexp/)
test_marshal("l+\n\000\000\000\000\000\000\000\000@\000", 2 ** 70)
#test_marshal("l+\f\313\220\263z\e\330p\260\200-\326\311\264\000",
#             14323534664547457526224437612747)
test_marshal("l+\n\001\000\001@\000\000\000\000@\000",
             1 + (2 ** 16) + (2 ** 30) + (2 ** 70))
test_marshal("l+\n6\361\3100_/\205\177Iq",
             534983213684351312654646)
test_marshal("l-\n6\361\3100_/\205\177Iq",
             -534983213684351312654646)
test_marshal("l+\n\331\347\365%\200\342a\220\336\220",
             684126354563246654351321)
#test_marshal("l+\vIZ\210*,u\006\025\304\016\207\001",
#            472759725676945786624563785)

test_marshal("c\023Struct::Froboz",
             Struct.new("Froboz", :x, :y))
test_marshal("S:\023Struct::Froboz\a:\006xi\n:\006yi\f",
             Struct::Froboz.new(5, 7))

# Can't dump anonymous class
#test_exception(ArgumentError) { Marshal.dump(Struct.new(:x, :y).new(5, 7)) }


# FIXME: Bignum marshaling is broken.

# FIXME: IVAR, MODULE_OLD, 'U', ...

test_marshal("o:\013Object\000", Object.new)
class MarshalTestClass
  def initialize
    @foo = "bar"
  end
end
test_marshal("o:\025MarshalTestClass\006:\t@foo\"\010bar",
             MarshalTestClass.new)
o = Object.new
test_marshal("[\to:\013Object\000@\006@\006@\006",
             [o, o, o, o])
class MarshalTestClass
  def initialize
    @foo = self
  end
end
test_marshal("o:\025MarshalTestClass\006:\t@foo@\000",
             MarshalTestClass.new)

class UserMarshaled
  attr :foo
  def initialize(foo)
    @foo = foo
  end
  class << self
    def _load(str)
      return self.new(str.reverse.to_i)
    end
  end
  def _dump(depth)
    @foo.to_s.reverse
  end
  def ==(other)
    self.class == other.class && self.foo == other.foo
  end
end
um = UserMarshaled.new(123)
test_marshal("u:\022UserMarshaled\010321", um)
test_equal(um, Marshal.load(Marshal.dump(um)))

test_marshal("[\a00", [nil, nil])
test_marshal("[\aTT", [true, true])
test_marshal("[\ai\006i\006", [1, 1])
test_marshal("[\a:\ahi;\000", [:hi, :hi])
o = Object.new
test_marshal("[\ao:\013Object\000@\006", [o, o])

test_exception(ArgumentError) {
  Marshal.load("\004\bu:\026SomeUnknownClassX\nhello")
}

module UM
  class UserMarshal
    def _dump(depth)
      "hello"
    end
  end
end
begin
  Marshal.load("\004\bu:\024UM::UserMarshal\nhello")
  test_fail
rescue TypeError => e
  test_equal("class UM::UserMarshal needs to have method `_load'", e.message)
end

# Unmarshaling

object = Marshal.load(MARSHAL_HEADER + "o:\025MarshalTestClass\006:\t@foo\"\010bar")
test_equal(["@foo"], object.instance_variables)

test_equal(true, Marshal.load(MARSHAL_HEADER + "T"))
test_equal(false, Marshal.load(MARSHAL_HEADER + "F"))
test_equal(nil, Marshal.load(MARSHAL_HEADER + "0"))
test_equal("hello", Marshal.load(MARSHAL_HEADER + "\"\nhello"))
test_equal(1, Marshal.load(MARSHAL_HEADER + "i\006"))
test_equal(-1, Marshal.load(MARSHAL_HEADER + "i\372"))
test_equal(-2, Marshal.load(MARSHAL_HEADER + "i\371"))
test_equal(2000, Marshal.load(MARSHAL_HEADER + "i\002\320\a"))
test_equal(-2000, Marshal.load(MARSHAL_HEADER + "i\3760\370"))
test_equal(1000000000, Marshal.load(MARSHAL_HEADER + "i\004\000\312\232;"))
test_equal([1, 2, 3], Marshal.load(MARSHAL_HEADER + "[\010i\006i\ai\010"))
test_equal({1=>2}, Marshal.load(MARSHAL_HEADER + "{\006i\006i\a"))
test_equal(String, Marshal.load(MARSHAL_HEADER + "c\013String"))
#test_equal(Enumerable, Marshal.load(MARSHAL_HEADER + "m\017Enumerable"))
test_equal(Foo::Bar, Marshal.load(MARSHAL_HEADER + "c\rFoo::Bar"))

s = Marshal.load(MARSHAL_HEADER + "S:\023Struct::Froboz\a:\006xi\n:\006yi\f")
test_equal(Struct::Froboz, s.class)
test_equal(5, s.x)
test_equal(7, s.y)

test_equal(2 ** 70, Marshal.load(MARSHAL_HEADER + "l+\n\000\000\000\000\000\000\000\000@\000"))

object = Marshal.load(MARSHAL_HEADER + "o:\013Object\000")
test_equal(Object, object.class)

Marshal.dump([1,2,3], 2)
test_exception(ArgumentError) { Marshal.dump([1,2,3], 1) }

o = Object.new
a = Marshal.load(Marshal.dump([o, o, o, o]))
test_ok(a[0] == a[1])
a = Marshal.load(Marshal.dump([:hi, :hi, :hi, :hi]))
test_ok(a[0] == :hi)
test_ok(a[1] == :hi)

# simple extensions of builtins should retain their types
class MyHash < Hash
  attr_accessor :used, :me
  
  def initialize
  	super
    @used = {}
    @me = 'a'
  end
  
  def []=(k, v) #:nodoc:
    @used[k] = false
    super
  end
  
  def foo; end
end

x = MyHash.new

test_equal(MyHash, Marshal.load(Marshal.dump(x)).class)
test_equal(x, Marshal.load(Marshal.dump(x)))

x['a'] = 'b'
#test_equal(x, Marshal.load(Marshal.dump(x)))

class F < Hash
  def initialize #:nodoc:
    super
    @val = { :notice=>true }
    @val2 = { :notice=>false }
  end
end

test_equal(F.new,Marshal.load(Marshal.dump(F.new)))

test_equal(4, Marshal::MAJOR_VERSION)
test_equal(8, Marshal::MINOR_VERSION)

