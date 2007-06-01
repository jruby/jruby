#####################################################################
# tc_sinh.rb
#
# Test cases for the Math.sinh method.
#####################################################################
require 'test/unit'

class TC_Math_Sinh_Class < Test::Unit::TestCase
   def test_sinh_basic
      assert_respond_to(Math, :sinh)
      assert_nothing_raised{ Math.sinh(1) }
      assert_nothing_raised{ Math.sinh(100) }
      assert_kind_of(Float, Math.sinh(1))
   end
   
   def test_sinh_positive
      assert_nothing_raised{ Math.sinh(1) }
      assert_in_delta(1.175, Math.sinh(1), 0.01)
   end
   
   def test_sinh_zero
      assert_nothing_raised{ Math.sinh(0) }
      assert_in_delta(0.0, Math.sinh(0), 0.01)
   end
   
   def test_sinh_negative
      assert_nothing_raised{ Math.sinh(-1) }
      assert_in_delta(-1.175, Math.sinh(-1), 0.01)
   end
   
   def test_sinh_positive_float
      assert_nothing_raised{ Math.sinh(0.345) }
      assert_in_delta(0.351, Math.sinh(0.345), 0.01)
   end
   
   def test_sinh_negative_float
      assert_nothing_raised{ Math.sinh(-0.345) }
      assert_in_delta(-0.351, Math.sinh(-0.345), 0.01)
   end

   def test_sinh_expected_errors
      assert_raises(TypeError){ Math.sinh(nil) }
      assert_raises(ArgumentError){ Math.sinh('test') }
   end
end
