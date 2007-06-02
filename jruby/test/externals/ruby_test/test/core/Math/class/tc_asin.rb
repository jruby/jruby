#####################################################################
# tc_asin.rb
#
# Test cases for the Math.asin method.
#####################################################################
require 'test/unit'

class TC_Math_Asin_Class < Test::Unit::TestCase
   def test_asin_basic
      assert_respond_to(Math, :asin)
      assert_nothing_raised{ Math.asin(1) }
      assert_kind_of(Float, Math.asin(1))
   end
   
   def test_asin_positive
      assert_nothing_raised{ Math.asin(1) }
      assert_in_delta(1.57, Math.asin(1), 0.01)
   end
   
   def test_asin_zero
      assert_nothing_raised{ Math.asin(0) }
      assert_equal(0.0, Math.asin(0))
   end
   
   def test_asin_negative
      assert_nothing_raised{ Math.asin(-1) }
      assert_in_delta(-1.57, Math.asin(-1), 0.01)
   end
   
   def test_asin_positive_float
      assert_nothing_raised{ Math.asin(0.345) }
      assert_in_delta(0.35, Math.asin(0.345), 0.01)
   end
   
   def test_asin_negative_float
      assert_nothing_raised{ Math.asin(-0.345) }
      assert_in_delta(-0.35, Math.asin(-0.345), 0.01)
   end
   
   def test_asin_expected_errors
       assert_raises(Errno::EDOM){ Math.asin(2) }
       assert_raises(Errno::EDOM){ Math.asin(-2) }
   end
end