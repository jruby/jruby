#####################################################################
# tc_cos.rb
#
# Test cases for the Math.cos method.
#####################################################################
require 'test/unit'

class TC_Math_Cos_Class < Test::Unit::TestCase
   def test_cos_basic
      assert_respond_to(Math, :cos)
      assert_nothing_raised{ Math.cos(1) }
      assert_kind_of(Float, Math.cos(1))
   end
   
   def test_cos_positive
      assert_nothing_raised{ Math.cos(1) }
      assert_in_delta(0.54, Math.cos(1), 0.01)
   end
   
   def test_cos_zero
      assert_nothing_raised{ Math.cos(0) }
      assert_in_delta(1.0, Math.cos(0), 0.01)
   end
   
   def test_cos_negative
      assert_nothing_raised{ Math.cos(-1) }
      assert_in_delta(0.54, Math.cos(-1), 0.01)
   end
   
   def test_cos_positive_float
      assert_nothing_raised{ Math.cos(0.345) }
      assert_in_delta(0.94, Math.cos(0.345), 0.01)
   end
   
   def test_cos_negative_float
      assert_nothing_raised{ Math.cos(-0.345) }
      assert_in_delta(0.94, Math.cos(-0.345), 0.01)
   end

   def test_cos_expected_errors
      assert_raises(TypeError){ Math.cos(nil) }
      assert_raises(ArgumentError){ Math.cos('test') }
   end
end
