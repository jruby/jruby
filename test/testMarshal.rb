require 'minirunit'
test_check "Test Marshal:"

MARSHAL_HEADER = "\004\005"

test_equal(MARSHAL_HEADER + "0", Marshal.dump(nil))
test_equal(MARSHAL_HEADER + "T", Marshal.dump(true))
test_equal(MARSHAL_HEADER + "F", Marshal.dump(false))
test_equal(MARSHAL_HEADER + "i\000", Marshal.dump(0))
test_equal(MARSHAL_HEADER + "i\006", Marshal.dump(1))
test_equal(MARSHAL_HEADER + "iú", Marshal.dump(-1))
test_equal(MARSHAL_HEADER + "i\002Ð\a", Marshal.dump(2000))
test_equal(MARSHAL_HEADER + "iþ0ø", Marshal.dump(-2000))
test_equal(MARSHAL_HEADER + "i\004\000Ê\232;", Marshal.dump(1000000000))
test_equal(MARSHAL_HEADER + ":\017somesymbol", Marshal.dump(:somesymbol))
test_equal(MARSHAL_HEADER + "f\n2.002", Marshal.dump(2.002))
test_equal(MARSHAL_HEADER + "f\013-2.002", Marshal.dump(-2.002))
test_equal(MARSHAL_HEADER + "\"\nhello", Marshal.dump("hello"))
test_equal(MARSHAL_HEADER + "[\010i\006i\ai\010", Marshal.dump([1,2,3]))
test_equal(MARSHAL_HEADER + "{\006i\006i\a", Marshal.dump({1=>2}))
test_equal(MARSHAL_HEADER + "c\013Object", Marshal.dump(Object))
test_equal(MARSHAL_HEADER + "m\017Enumerable", Marshal.dump(Enumerable))
test_equal(MARSHAL_HEADER + "/\013regexp\000", Marshal.dump(/regexp/))
#  test_equal(MARSHAL_HEADER + "l+\n\000\000\000\000\000\000\000\000@\000", Marshal.dump(2 ** 70))
#  test_equal(MARSHAL_HEADER + "l+\f\313\220\263z\e\330p\260\200-\326\311\264\000",
#             Marshal.dump(14323534664547457526224437612747))
#  test_equal(MARSHAL_HEADER + "l+\n\001\000\001@\000\000\000\000@\000",
#             Marshal.dump(1 + (2 ** 16) + (2 ** 30) + (2 ** 70)))
#  test_equal(MARSHAL_HEADER + "l+\n6\361\3100_/\205\177Iq",
#             Marshal.dump(534983213684351312654646))
#  test_equal(MARSHAL_HEADER + "l-\n6\361\3100_/\205\177Iq",
#             Marshal.dump(-534983213684351312654646))
#  test_equal(MARSHAL_HEADER + "l+\n\331\347\365%\200\342a\220\336\220",
#             Marshal.dump(684126354563246654351321))
#  test_equal(MARSHAL_HEADER + "l+\vIZ\210*,u\006\025\304\016\207\001",
#             Marshal.dump(472759725676945786624563785))

# FIXME: IVAR, struct, MODULE_OLD, 'U', ...

test_equal(MARSHAL_HEADER + "o:\013Object\000", Marshal.dump(Object.new))
class MarshalTestClass
  def initialize
    @foo = "bar"
  end
end
test_equal(MARSHAL_HEADER + "o:\025MarshalTestClass\006:\t@foo\"\010bar",
	   Marshal.dump(MarshalTestClass.new))
o = Object.new
test_equal(MARSHAL_HEADER + "[\to:\013Object\000@\006@\006@\006",
	   Marshal.dump([o, o, o, o]))
class MarshalTestClass
  def initialize
    @foo = self
  end
end
test_equal(MARSHAL_HEADER + "o:\025MarshalTestClass\006:\t@foo@\000",
	   Marshal.dump(MarshalTestClass.new))

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
test_equal(MARSHAL_HEADER + "u:\022UserMarshaled\010321", Marshal.dump(um))
test_equal(um, Marshal.load(Marshal.dump(um)))


# Unmarshaling

object = Marshal.load(MARSHAL_HEADER + "o:\025MarshalTestClass\006:\t@foo\"\010bar")
test_equal(["@foo"], object.instance_variables)
test_equal(true, Marshal.load(MARSHAL_HEADER + "T"))
test_equal(false, Marshal.load(MARSHAL_HEADER + "F"))
test_equal(nil, Marshal.load(MARSHAL_HEADER + "0"))
test_equal("hello", Marshal.load(MARSHAL_HEADER + "\"\nhello"))
test_equal(1, Marshal.load(MARSHAL_HEADER + "i\006"))
test_equal(-1, Marshal.load(MARSHAL_HEADER + "iú"))
test_equal(-2, Marshal.load(MARSHAL_HEADER + "iù"))
test_equal(2000, Marshal.load(MARSHAL_HEADER + "i\002Ð\a"))
test_equal(-2000, Marshal.load(MARSHAL_HEADER + "iþ0ø"))
test_equal(1000000000, Marshal.load(MARSHAL_HEADER + "i\004\000Ê\232;"))
test_equal([1, 2, 3], Marshal.load(MARSHAL_HEADER + "[\010i\006i\ai\010"))
test_equal({1=>2}, Marshal.load(MARSHAL_HEADER + "{\006i\006i\a"))
test_equal(String, Marshal.load(MARSHAL_HEADER + "c\013String"))
#test_equal(Enumerable, Marshal.load(MARSHAL_HEADER + "m\017Enumerable"))

object = Marshal.load(MARSHAL_HEADER + "o:\013Object\000")
test_equal(Object, object.class)

Marshal.dump([1,2,3], 2)
test_exception(ArgumentError) { Marshal.dump([1,2,3], 1) }
