#####################################################################
# tc_erf.rb
#
# Test cases for the Math.erf method.
#####################################################################
require 'test/unit'

class TC_Math_Erf_Class < Test::Unit::TestCase
   def test_erf_basic
      assert_respond_to(Math, :erf)
      assert_nothing_raised{ Math.erf(1) }
      assert_kind_of(Float, Math.erf(1))
   end
   
   def test_erf_positive
      assert_nothing_raised{ Math.erf(1) }
      assert_in_delta(0.84, Math.erf(1), 0.01)
   end
   
   def test_erf_zero
      assert_nothing_raised{ Math.erf(0) }
      assert_in_delta(0.0, Math.erf(0), 0.01)
   end
   
   def test_erf_negative
      assert_nothing_raised{ Math.erf(-1) }
      assert_in_delta(-0.84, Math.erf(-1), 0.01)
   end
   
   def test_erf_positive_float
      assert_nothing_raised{ Math.erf(0.345) }
      assert_in_delta(0.374, Math.erf(0.345), 0.01)
   end
   
   def test_erf_negative_float
      assert_nothing_raised{ Math.erf(-0.345) }
      assert_in_delta(-0.374, Math.erf(-0.345), 0.01)
   end

   def test_erf_expected_errors
      assert_raises(ArgumentError){ Math.erf('test') }
      assert_raises(TypeError){ Math.erf(nil) }
   end
end
