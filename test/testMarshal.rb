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
test_marshal("l+\f\313\220\263z\e\330p\260\200-\326\311\264\000",
             14323534664547457526224437612747)
test_marshal("l+\n\001\000\001@\000\000\000\000@\000",
             1 + (2 ** 16) + (2 ** 30) + (2 ** 70))
test_marshal("l+\n6\361\3100_/\205\177Iq",
             534983213684351312654646)
test_marshal("l-\n6\361\3100_/\205\177Iq",
             -534983213684351312654646)
test_marshal("l+\n\331\347\365%\200\342a\220\336\220",
             684126354563246654351321)
test_marshal("l+\vIZ\210*,u\006\025\304\016\207\001",
            472759725676945786624563785)

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
test_equal(x, Marshal.load(Marshal.dump(x)))

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

# Hashes with defaults serialize a bit differently; confirm the default is coming back correctly
x = {}
x.default = "foo"
test_equal("foo", Marshal.load(Marshal.dump(x)).default)

# Range tests
x = 1..10
y = Marshal.load(Marshal.dump(x))
test_equal(x, y)
test_equal(x.class, y.class)
test_no_exception {
  test_equal(10, y.max)
  test_equal(1, y.min)
  test_equal(false, y.exclude_end?)
  y.each {}
}
z = Marshal.dump(x)
test_ok(z.include?("excl"))
test_ok(z.include?("begin"))
test_ok(z.include?("end"))

def test_core_subclass_marshalling(type, *init_args)
  my_type = nil
  eval <<-EOS
  class My#{type} < #{type}
    attr_accessor :foo
    def initialize(*args)
      super
      @foo = "hello"
    end
  end
  my_type = My#{type}
  EOS

  x = my_type.new(*init_args)
  y = Marshal.load(Marshal.dump(x))
  test_equal(my_type, y.class)
  test_no_exception {
    test_equal(x.to_s, y.to_s)
    test_equal("hello", y.foo)
  }
end

test_core_subclass_marshalling(Range, 1, 10)
test_core_subclass_marshalling(Array, 0)
test_core_subclass_marshalling(Hash, 5)
test_core_subclass_marshalling(Regexp, //)

# FIXME: this isn't working because we intercept system calls to "ruby" and run JRuby...
=begin
ruby_available = (`ruby -v`[0..3] == "ruby")

if ruby_available
  def test_dump_against_ruby(eval_string)
    ruby_command = "ruby -e 'p Marshal.dump(eval\"#{eval_string}\")'"
    ruby_output = "Ruby output: " + system(ruby_command)

    test_equal(ruby_output, Marshal.dump(eval(eval_string)))
  end
else
  def test_dump_against_ruby(eval_string)
    warn "ruby interpreter not available, skipping test
  end
end

test_dump_against_ruby("Object.new")
=end

# Time is user-marshalled, so ensure it's being marshalled correctly
x = Time.now
y = Marshal.dump([x,x])
# symlink for second time object
test_equal(6, y[-1])
test_equal([x, x], Marshal.load(y))

# User-marshalled classes should marshal singleton objects as the original class
class Special  
  def initialize(valuable)
    @valuable = valuable
  end

  def _dump(depth)
    @valuable.to_str
  end

  def Special._load(str)
    result = Special.new(str);
  end
end

a = Special.new("Hello, World")
class << a
  def newMeth
    puts "HELLO"
  end
end
data = Marshal.dump(a)
test_equal("\004\bu:\fSpecial\021Hello, World", data)
test_no_exception { obj = Marshal.load(data) }

class Aaaa < Array
  attr_accessor :foo
end
a = Aaaa.new
a.foo = :Aaaa
test_marshal("IC:\tAaaa[\000\006:\t@foo;\000",a)

# Check that exception message and backtrace are preserved
class SomeException < Exception
  def initialize(message)
    super(message)
  end
  # Also check that subclass ivars are preserved
  attr_accessor :ivar
end

# Create an exception, set a fixed backtrace
e1 = SomeException.new("a message")
e1.set_backtrace ["line 1", "line 2"]
e1.ivar = 42
e2 = Marshal.load(Marshal.dump(e1))
test_equal("a message", e2.message)
test_equal(e1.backtrace, e2.backtrace)
test_equal(42, e2.ivar)

# The following dump is generated by MRI
s = "\004\010o:\022SomeException\010:\tmesg\"\016a message:\abt[\a\"\13line 1\"\13line 2:\n@ivari/"
e3 = Marshal.load(s)
# Check that the MRI format loads correctly
test_equal("a message", e3.message)
test_equal(e1.backtrace, e3.backtrace)
test_equal(42, e3.ivar)

require 'stringio'
class MTStream < StringIO
  attr :binmode_called

  def binmode
    @binmode_called = true
  end
end

class BinmodeLessMTStream < StringIO
  undef_method :binmode
end

# Checking for stream
begin
  Marshal.dump("hi", :not_an_io)
rescue TypeError
else
  test_fail
end

# Writing to non-IO stream
stream = MTStream.new
Marshal.dump("hi", stream)
test_ok(stream.size > 0)

# Calling binmode if available
stream = MTStream.new
Marshal.dump("hi", stream)
test_ok(stream.binmode_called)

# Ignoring binmode if unavailable
stream = BinmodeLessMTStream.new
Marshal.dump("hi", stream)

# Loading from non-IO stream
stream = MTStream.new
Marshal.dump("hi", stream)
stream.rewind
test_equal("hi", Marshal.load(stream))

# Setting binmode on input
stream = MTStream.new
Marshal.dump("hi", stream)
stream.rewind
s = stream.read
in_stream = MTStream.new
in_stream.write(s)
in_stream.rewind
Marshal.load(in_stream)
test_ok(in_stream.binmode_called)

# thread isn't marshalable
test_exception(TypeError) { Marshal.dump(Thread.new {}) }