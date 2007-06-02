#####################################################################
# tc_atan.rb
#
# Test cases for the Math.atan method.
#####################################################################
require 'test/unit'

class TC_Math_Atan_Class < Test::Unit::TestCase
   def test_atan_basic
      assert_respond_to(Math, :atan)
      assert_nothing_raised{ Math.atan(1) }
      assert_kind_of(Float, Math.atan(1))
   end
   
   def test_atan_positive
      assert_nothing_raised{ Math.atan(1) }
      assert_nothing_raised{ Math.atan(100) }
      assert_in_delta(0.785, Math.atan(1), 0.01)
   end
   
   def test_atan_zero
      assert_nothing_raised{ Math.atan(0) }
      assert_equal(0.0, Math.atan(0))
   end
   
   def test_atan_negative
      assert_nothing_raised{ Math.atan(-1) }
      assert_nothing_raised{ Math.atan(-100) }
      assert_in_delta(-0.785, Math.atan(-1), 0.01)
   end
   
   def test_atan_positive_float
      assert_nothing_raised{ Math.atan(0.345) }
      assert_in_delta(0.332, Math.atan(0.345), 0.01)
   end
   
   def test_atan_negative_float
      assert_nothing_raised{ Math.atan(-0.345) }
      assert_in_delta(-0.332, Math.atan(-0.345), 0.01)
   end
end
