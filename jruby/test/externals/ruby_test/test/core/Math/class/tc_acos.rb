#####################################################################
# tc_acos.rb
#
# Test cases for the Math.acos method.
#####################################################################
require 'test/unit'

class TC_Math_Acos_Class < Test::Unit::TestCase
   def test_acos_basic
      assert_respond_to(Math, :acos)
      assert_nothing_raised{ Math.acos(1) }
      assert_kind_of(Float, Math.acos(1))
   end
   
   def test_acos_positive
      assert_nothing_raised{ Math.acos(1) }
      assert_equal(0.0, Math.acos(1))
   end
   
   def test_acos_zero
      assert_nothing_raised{ Math.acos(0) }
      assert_in_delta(1.57, Math.acos(0), 0.01)
   end
   
   def test_acos_negative
      assert_nothing_raised{ Math.acos(-1) }
      assert_in_delta(3.14, Math.acos(-1), 0.01)
   end
   
   def test_acos_positive_float
      assert_nothing_raised{ Math.acos(0.345) }
      assert_in_delta(1.21, Math.acos(0.345), 0.01)
   end
   
   def test_acos_negative_float
      assert_nothing_raised{ Math.acos(-0.345) }
      assert_in_delta(1.92, Math.acos(-0.345), 0.01)
   end
   
   def test_acos_expected_errors
       assert_raises(Errno::EDOM){ Math.acos(2) }
       assert_raises(Errno::EDOM){ Math.acos(-2) }
   end
end