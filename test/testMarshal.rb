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
# FIXME: Test limits of fixnum
test_equal(MARSHAL_HEADER + ":\017somesymbol", Marshal.dump(:somesymbol))
test_equal(MARSHAL_HEADER + "f\n2.002", Marshal.dump(2.002))
test_equal(MARSHAL_HEADER + "f\013-2.002", Marshal.dump(-2.002))
test_equal(MARSHAL_HEADER + "\"\nhello", Marshal.dump("hello"))
test_equal(MARSHAL_HEADER + "[\010i\006i\ai\010", Marshal.dump([1,2,3]))
test_equal(MARSHAL_HEADER + "{\006i\006i\a", Marshal.dump({1=>2}))
test_equal(MARSHAL_HEADER + "c\013Object", Marshal.dump(Object))
test_equal(MARSHAL_HEADER + "m\017Enumerable", Marshal.dump(Enumerable))
test_equal(MARSHAL_HEADER + "/\013regexp\000", Marshal.dump(/regexp/))

# FIXME: bignum, usermarshal, ...

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


test_equal(true, Marshal.load(MARSHAL_HEADER + "T"))
test_equal(false, Marshal.load(MARSHAL_HEADER + "F"))
test_equal(nil, Marshal.load(MARSHAL_HEADER + "0"))
test_equal("hello", Marshal.load(MARSHAL_HEADER + "\"\nhello"))
test_equal(1, Marshal.load(MARSHAL_HEADER + "i\006"))
test_equal(-1, Marshal.load(MARSHAL_HEADER + "iú"))
test_equal(-2, Marshal.load(MARSHAL_HEADER + "iù"))
test_equal(2000, Marshal.load(MARSHAL_HEADER + "i\002Ð\a"))
test_equal(-2000, Marshal.load(MARSHAL_HEADER + "iþ0ø"))
test_equal([1, 2, 3], Marshal.load(MARSHAL_HEADER + "[\010i\006i\ai\010"))
test_equal({1=>2}, Marshal.load(MARSHAL_HEADER + "{\006i\006i\a"))
object = Marshal.load(MARSHAL_HEADER + "o:\013Object\000")
test_equal(Object, object.class)

exception_thrown = false
begin
  Marshal.dump([1,2,3], 1)
rescue ArgumentError
  exception_thrown = true  
end
test_ok(exception_thrown)


exception_thrown = false
begin
  Marshal.dump([1,2,3], 2)
rescue ArgumentError
  exception_thrown = true  
end
test_ok(!exception_thrown)
