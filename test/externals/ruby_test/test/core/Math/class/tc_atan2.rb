#####################################################################
# tc_atan2.rb
#
# Test cases for the Math.atan2 method.
#####################################################################
require 'test/unit'

class TC_Math_Atan2_Class < Test::Unit::TestCase
   def test_atan2_basic
      assert_respond_to(Math, :atan2)
      assert_nothing_raised{ Math.atan2(0, 1) }
      assert_kind_of(Float, Math.atan2(0, 1))
   end
   
   def test_atan2_positive
      assert_nothing_raised{ Math.atan2(1, 2) }
      assert_nothing_raised{ Math.atan2(1, 100) }
      assert_in_delta(0.463, Math.atan2(1, 2), 0.01)
   end
   
   def test_atan2_zero
      assert_nothing_raised{ Math.atan2(0, 0) }
      assert_equal(0.0, Math.atan2(0, 0))
   end
   
   def test_atan2_negative
      assert_nothing_raised{ Math.atan2(-2, -1) }
      assert_nothing_raised{ Math.atan2(-1, -2) }
      assert_nothing_raised{ Math.atan2(-2, -100) }
      assert_in_delta(-2.03, Math.atan2(-2, -1), 0.01)
   end
   
   def test_atan2_positive_float
      assert_nothing_raised{ Math.atan2(0.345, 0.745) }
      assert_in_delta(0.433, Math.atan2(0.345, 0.745), 0.01)
   end
   
   def test_atan2_negative_float
      assert_nothing_raised{ Math.atan2(-0.345, -0.745) }
      assert_in_delta(-2.70, Math.atan2(-0.345, -0.745), 0.01)
   end
end
