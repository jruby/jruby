#####################################################################
# tc_sqrt.rb
#
# Test cases for the Math.sqrt method.
#####################################################################
require 'test/unit'

class TC_Math_Sqrt_Class < Test::Unit::TestCase
   def test_sqrt_basic
      assert_respond_to(Math, :sqrt)
      assert_nothing_raised{ Math.sqrt(1) }
      assert_nothing_raised{ Math.sqrt(100) }
      assert_kind_of(Float, Math.sqrt(1))
   end

   def test_sqrt_positive
      assert_nothing_raised{ Math.sqrt(1) }
      assert_in_delta(1.0, Math.sqrt(1), 0.01)
   end

   def test_sqrt_zero
      assert_nothing_raised{ Math.sqrt(0) }
      assert_in_delta(0.0, Math.sqrt(0), 0.01)
   end
   
   def test_sqrt_positive_float
      assert_nothing_raised{ Math.sqrt(0.345) }
      assert_in_delta(0.58, Math.sqrt(0.345), 0.01)
   end
   
   # TODO: Shouldn't all non-numerics raise TypeError?
   def test_sqrt_expected_errors
      assert_raises(Errno::EDOM){ Math.sqrt(-1) }
      assert_raises(TypeError){ Math.sqrt(nil) }
      assert_raises(ArgumentError){ Math.sqrt('test') }
   end
end
