#####################################################################
# tc_cosh.rb
#
# Test cases for the Math.cosh method.
#####################################################################
require 'test/unit'

class TC_Math_Cosh_Class < Test::Unit::TestCase
   def test_cosh_basic
      assert_respond_to(Math, :cosh)
      assert_nothing_raised{ Math.cosh(1) }
      assert_nothing_raised{ Math.cosh(100) }
      assert_kind_of(Float, Math.cosh(1))
   end
   
   def test_cosh_positive
      assert_nothing_raised{ Math.cosh(1) }
      assert_in_delta(1.54, Math.cosh(1), 0.01)
   end
   
   def test_cosh_zero
      assert_nothing_raised{ Math.cosh(0) }
      assert_in_delta(1.0, Math.cosh(0), 0.01)
   end
   
   def test_cosh_negative
      assert_nothing_raised{ Math.cosh(-1) }
      assert_in_delta(1.54, Math.cosh(-1), 0.01)
   end
   
   def test_cosh_positive_float
      assert_nothing_raised{ Math.cosh(0.345) }
      assert_in_delta(1.06, Math.cosh(0.345), 0.01)
   end
   
   def test_cosh_negative_float
      assert_nothing_raised{ Math.cosh(-0.345) }
      assert_in_delta(1.06, Math.cosh(-0.345), 0.01)
   end

   # TODO: Shouldn't they both raise a TypeError?
   def test_cosh_expected_errors
      assert_raises(ArgumentError){ Math.cosh('test') }
      assert_raises(TypeError){ Math.cosh(nil) }
   end
end
