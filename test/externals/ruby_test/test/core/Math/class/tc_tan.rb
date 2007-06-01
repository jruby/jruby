#####################################################################
# tc_tan.rb
#
# Test cases for the Math.tan method.
#####################################################################
require 'test/unit'

class TC_Math_Tan_Class < Test::Unit::TestCase
   def test_tan_basic
      assert_respond_to(Math, :tan)
      assert_nothing_raised{ Math.tan(1) }
      assert_kind_of(Float, Math.tan(1))
   end
   
   def test_tan_positive
      assert_nothing_raised{ Math.tan(1) }
      assert_in_delta(1.55, Math.tan(1), 0.01)
   end
   
   def test_tan_zero
      assert_nothing_raised{ Math.tan(0) }
      assert_in_delta(0.0, Math.tan(0), 0.01)
   end
   
   def test_tan_negative
      assert_nothing_raised{ Math.tan(-1) }
      assert_in_delta(-1.55, Math.tan(-1), 0.01)
   end
   
   def test_tan_positive_float
      assert_nothing_raised{ Math.tan(0.345) }
      assert_in_delta(0.35, Math.tan(0.345), 0.01)
   end
   
   def test_tan_negative_float
      assert_nothing_raised{ Math.tan(-0.345) }
      assert_in_delta(-0.35, Math.tan(-0.345), 0.01)
   end

   def test_tan_expected_errors
      assert_raises(TypeError){ Math.tan(nil) }
      assert_raises(ArgumentError){ Math.tan('test') }
   end
end
