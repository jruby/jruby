#####################################################################
# tc_hypot.rb
#
# Test cases for the Math.hypot method.
#####################################################################
require 'test/unit'

class TC_Math_Hypot_Class < Test::Unit::TestCase
   def test_hypot_basic
      assert_respond_to(Math, :hypot)
      assert_nothing_raised{ Math.hypot(1, 2) }
      assert_kind_of(Float, Math.hypot(1, 2))
   end
   
   def test_hypot_positive
      assert_nothing_raised{ Math.hypot(1, 2) }
      assert_in_delta(2.23, Math.hypot(1, 2), 0.01)
   end
   
   def test_hypot_zero
      assert_nothing_raised{ Math.hypot(0, 0) }
      assert_in_delta(0.0, Math.hypot(0, 0), 0.01)
   end
   
   def test_hypot_negative
      assert_nothing_raised{ Math.hypot(-1, -2) }
      assert_in_delta(2.23, Math.hypot(-1, -2), 0.01)
   end
   
   def test_hypot_positive_float
      assert_nothing_raised{ Math.hypot(0.345, 0.655) }
      assert_in_delta(0.74, Math.hypot(0.345, 0.655), 0.01)
   end
   
   def test_hypot_negative_float
      assert_nothing_raised{ Math.hypot(-0.345, -0.655) }
      assert_in_delta(0.74, Math.hypot(-0.345, -0.655), 0.01)
   end

   def test_hypot_expected_errors
      assert_raises(ArgumentError){ Math.hypot(0) }
      assert_raises(TypeError){ Math.hypot(nil, nil) }
   end
end
