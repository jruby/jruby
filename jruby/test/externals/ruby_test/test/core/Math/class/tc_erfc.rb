#####################################################################
# tc_erfc.rb
#
# Test cases for the Math.erfc method.
#####################################################################
require 'test/unit'

class TC_Math_Erfc_Class < Test::Unit::TestCase
   def test_erfc_basic
      assert_respond_to(Math, :erfc)
      assert_nothing_raised{ Math.erfc(1) }
      assert_kind_of(Float, Math.erfc(1))
   end
   
   def test_erfc_positive
      assert_nothing_raised{ Math.erfc(1) }
      assert_in_delta(0.157, Math.erfc(1), 0.01)
   end
   
   def test_erfc_zero
      assert_nothing_raised{ Math.erfc(0) }
      assert_in_delta(1.0, Math.erfc(0), 0.01)
   end
   
   def test_erfc_negative
      assert_nothing_raised{ Math.erfc(-1) }
      assert_in_delta(1.84, Math.erfc(-1), 0.01)
   end
   
   def test_erfc_positive_float
      assert_nothing_raised{ Math.erfc(0.345) }
      assert_in_delta(0.625, Math.erfc(0.345), 0.01)
   end
   
   def test_erfc_negative_float
      assert_nothing_raised{ Math.erfc(-0.345) }
      assert_in_delta(1.37, Math.erfc(-0.345), 0.01)
   end

   def test_erfc_expected_errors
      assert_raises(ArgumentError){ Math.erfc('test') }
      assert_raises(TypeError){ Math.erfc(nil) }
   end
end
