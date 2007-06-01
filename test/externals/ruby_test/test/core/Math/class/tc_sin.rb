#####################################################################
# tc_sin.rb
#
# Test cases for the Math.sin method.
#####################################################################
require 'test/unit'

class TC_Math_Sin_Class < Test::Unit::TestCase
   def test_sin_basic
      assert_respond_to(Math, :sin)
      assert_nothing_raised{ Math.sin(1) }
      assert_nothing_raised{ Math.sin(100) }
      assert_kind_of(Float, Math.sin(1))
   end
   
   def test_sin_positive
      assert_nothing_raised{ Math.sin(1) }
      assert_in_delta(0.84, Math.sin(1), 0.01)
   end
   
   def test_sin_zero
      assert_nothing_raised{ Math.sin(0) }
      assert_in_delta(0.0, Math.sin(0), 0.01)
   end
   
   def test_sin_negative
      assert_nothing_raised{ Math.sin(-1) }
      assert_in_delta(-0.84, Math.sin(-1), 0.01)
   end
   
   def test_sin_positive_float
      assert_nothing_raised{ Math.sin(0.345) }
      assert_in_delta(0.338, Math.sin(0.345), 0.01)
   end
   
   def test_sin_negative_float
      assert_nothing_raised{ Math.sin(-0.345) }
      assert_in_delta(-0.338, Math.sin(-0.345), 0.01)
   end

   def test_sin_expected_errors
      assert_raises(TypeError){ Math.sin(nil) }
      assert_raises(ArgumentError){ Math.sin('test') }
   end
end
