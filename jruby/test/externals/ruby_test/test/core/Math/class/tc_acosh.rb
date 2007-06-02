#####################################################################
# tc_acosh.rb
#
# Test cases for the Math.acosh method.
#####################################################################
require 'test/unit'

class TC_Math_Acosh_Class < Test::Unit::TestCase
   def test_acosh_basic
      assert_respond_to(Math, :acosh)
      assert_nothing_raised{ Math.acosh(1) }
      assert_kind_of(Float, Math.acosh(1))
   end
   
   def test_acosh_positive
      assert_equal(0.0, Math.acosh(1))
      assert_in_delta(1.31, Math.acosh(2), 0.01)
      assert_in_delta(5.28, Math.acosh(99), 0.01)
   end
   
   def test_acosh_expected_errors
       assert_raises(Errno::EDOM){ Math.acosh(0) }
       assert_raises(Errno::EDOM){ Math.acosh(-1) }
   end
end