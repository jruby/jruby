######################################################################
# tc_coerce.rb
#
# Test case for the the Numeric#coerce instance method.
######################################################################
require 'test/unit'

# This is used to test coerced strings later.
class String
   def coerce(other)
      case other
      when Integer
         begin
            return other, Integer(self)
         rescue
            return Float(other), Float(self)
         end
      when Float
         return other, Float(self)
      else
         super
      end
   end
end

class TC_Numeric_Coerce_InstanceMethod < Test::Unit::TestCase
   def setup
      @integer = 1
      @float   = 2.5
   end

   def test_coerce_basic
      assert_respond_to(@integer, :coerce)
      assert_nothing_raised{ @integer.coerce(@integer) }
      assert_kind_of(Array, @integer.coerce(@integer))
   end

   def test_coerce_integer
      assert_equal([1, 1], @integer.coerce(@integer))
      assert_equal([2.5, 1.0], @integer.coerce(@float))
   end

   def test_coerce_float
      assert_equal([2.5, 2.5], @float.coerce(@float))
      assert_equal([1, 2.5], @float.coerce(@integer))
   end

   def test_coerce_zero
      assert_equal([0, 1], @integer.coerce(0))
      assert_equal([0, 2.5], @float.coerce(0))
   end

   def test_coerce_into_bignum
      assert_equal([4294967296, 1], @integer.coerce(4294967296))
      assert_equal([4294967296.0, 2.5], @float.coerce(4294967296))
   end

   def test_coerced_string
      assert_equal(3, 1 + "2")
      assert_in_delta(-1.3, 1 - "2.3", 0.001)
      assert_equal(3.5, 1.2 + "2.3")
      assert_equal(0, 1 - "1")
   end

   def test_coerce_expected_errors
      assert_raises(ArgumentError){ @integer.coerce("test") }
      assert_raises(TypeError){ @integer.coerce(nil) }
      assert_raises(TypeError){ @integer.coerce(false) }
   end

   def teardown
      @integer = nil
      @float   = nil
   end
end
