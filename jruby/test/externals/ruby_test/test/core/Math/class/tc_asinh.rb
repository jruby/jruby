#####################################################################
# tc_asinh.rb
#
# Test cases for the Math.asinh method.
#####################################################################
require 'test/unit'

class TC_Math_Asinh_Class < Test::Unit::TestCase
   def test_asinh_basic
      assert_respond_to(Math, :asinh)
      assert_nothing_raised{ Math.asinh(1) }
      assert_kind_of(Float, Math.asinh(1))
   end
   
   def test_asinh_positive
      assert_nothing_raised{ Math.asinh(1) }
      assert_nothing_raised{ Math.asinh(100) }
      assert_in_delta(0.88, Math.asinh(1), 0.01)
   end

   def test_asinh_zero
      assert_nothing_raised{ Math.asinh(0) }
      assert_equal(0.0, Math.asinh(0))
   end

   def test_asinh_negative
      assert_nothing_raised{ Math.asinh(-1) }
      assert_nothing_raised{ Math.asinh(-100) }
      assert_in_delta(-0.88, Math.asinh(-1), 0.01)
   end
   
   def test_asinh_positive_float
      assert_nothing_raised{ Math.asinh(0.345) }
      assert_in_delta(0.338, Math.asinh(0.345), 0.01)
   end
   
   def test_asinh_negative_float
      assert_nothing_raised{ Math.asinh(-0.345) }
      assert_in_delta(-0.338, Math.asinh(-0.345), 0.01)
   end
end
