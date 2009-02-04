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

test_exception(ArgumentError) { Marshal.load("\004\010U:\eCompletelyUnknownClass\"\nboing") }

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

# Check that numbers of all sizes don't make link indexes break
fixnum = 12345
mri_bignum = 1234567890
jruby_bignum = 12345678901234567890
s = "should be cached"
a = [fixnum,mri_bignum,jruby_bignum,s]
a = a + a
dumped_by_mri = "\004\b[\ri\00290l+\a\322\002\226Il+\t\322\n\037\353\214\251T\253\"\025should be cachedi\00290l+\a\322\002\226I@\a@\b"
test_no_exception { test_equal(a, Marshal.load(Marshal.dump(a))) }
test_no_exception { test_equal(a, Marshal.load(dumped_by_mri)) }
test_no_exception { test_equal(dumped_by_mri, Marshal.dump(a)) }

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

# time marshalling
unpacked_marshaled_time = [4, 8, 117, 58, 9, 84, 105, 109, 101, 13, 247, 239, 26, 192, 57, 48, 112, 57]
actual_time = Time.utc(2007, 12, 31, 23, 14, 23, 12345)
test_equal(
  unpacked_marshaled_time,
  Marshal.dump(actual_time).unpack('C*'))
test_equal(
  0,
  actual_time <=> Marshal.load(unpacked_marshaled_time.pack('C*')))

# JRUBY-2392
time = Time.now
is_utc = time.to_s =~ /UTC/
if (is_utc)
  test_ok(Marshal.load(Marshal.dump(time)).to_s =~ /UTC/)
else
  test_ok(!(Marshal.load(Marshal.dump(time)).to_s =~ /UTC/))
end

# JRUBY-2345
begin
  file_name = "test-file-tmp"
  f = File.new(file_name, "w+")
  test_exception(EOFError) { Marshal.load(f) }
ensure
  f.close
  File.delete(file_name)
end

test_exception(ArgumentError) { Marshal.load("\004\b\"\b") }

# borrowed from MRI 1.9 test:
class C
  def initialize(str)
    @str = str
  end
  def _dump(limit)
    @str
  end
  def self._load(s)
    new(s)
  end
end
test_exception(ArgumentError) {
  (data = Marshal.dump(C.new("a")))[-2, 1] = "\003\377\377\377"
  Marshal.load(data)
}

# JRUBY-2975: Overriding Time._dump does not behave the same as MRI
class Time
  class << self
    alias_method :_original_load, :_load
    def _load(marshaled_time)
      time = _original_load(marshaled_time)
      utc = time.send(:remove_instance_variable, '@marshal_with_utc_coercion')
      utc ? time.utc : time
    end
  end

  alias_method :_original_dump, :_dump
  def _dump(*args)
    obj = self.frozen? ? self.dup : self
    obj.instance_variable_set('@marshal_with_utc_coercion', utc?)
    obj._original_dump(*args)
  end
end

t = Time.local(2000).freeze
t2 = Marshal.load(Marshal.dump(t))
test_equal t, t2

# reset _load and _dump
class Time
  class << self
    alias_method :load, :_original_load
  end
  alias_method :dump, :_original_dump
end

# JRUBY-3366
# test wrong marshalling version
test_exception(TypeError) do 
  Marshal.load("\266q`?<??WO??<\376O\020?e\r\221D\e\200?_\210H\006:\037@marshal_with_utc_coercionF:\tsome\"\tdata")
end

