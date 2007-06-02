#####################################################################
# tc_tanh.rb
#
# Test cases for the Math.tanh method.
#####################################################################
require 'test/unit'

class TC_Math_Tanh_Class < Test::Unit::TestCase
   def test_tanh_basic
      assert_respond_to(Math, :tanh)
      assert_nothing_raised{ Math.tanh(1) }
      assert_kind_of(Float, Math.tanh(1))
   end
   
   def test_tanh_positive
      assert_nothing_raised{ Math.tanh(1) }
      assert_in_delta(0.76, Math.tanh(1), 0.01)
   end
   
   def test_tanh_zero
      assert_nothing_raised{ Math.tanh(0) }
      assert_in_delta(0.0, Math.tanh(0), 0.01)
   end
   
   def test_tanh_negative
      assert_nothing_raised{ Math.tanh(-1) }
      assert_in_delta(-0.76, Math.tanh(-1), 0.01)
   end
   
   def test_tanh_positive_float
      assert_nothing_raised{ Math.tanh(0.345) }
      assert_in_delta(0.33, Math.tanh(0.345), 0.01)
   end
   
   def test_tanh_negative_float
      assert_nothing_raised{ Math.tanh(-0.345) }
      assert_in_delta(-0.33, Math.tanh(-0.345), 0.01)
   end

   def test_tanh_expected_errors
      assert_raises(TypeError){ Math.tanh(nil) }
      assert_raises(ArgumentError){ Math.tanh('test') }
   end
end
