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
# FIXME: Test bigger numeric values
test_equal(MARSHAL_HEADER + ":\017somesymbol", Marshal.dump(:somesymbol))
test_equal(MARSHAL_HEADER + "f\n2.002", Marshal.dump(2.002))
test_equal(MARSHAL_HEADER + "f\013-2.002", Marshal.dump(-2.002))
test_equal(MARSHAL_HEADER + "\"\nhello", Marshal.dump("hello"))

# FIXME: bignum, array, hash, class, regexp, ...

test_equal(MARSHAL_HEADER + "o:\013Object\000", Marshal.dump(Object.new))
class MarshalTestClass
  def initialize
    @foo = "bar"
  end
end
test_equal(MARSHAL_HEADER + "o:\025MarshalTestClass\006:\t@foo\"\010bar",
	   Marshal.dump(MarshalTestClass.new))
