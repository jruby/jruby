#####################################################################
# tc_exp.rb
#
# Test cases for the Math.exp method.
#####################################################################
require 'test/unit'

class TC_Math_Exp_Class < Test::Unit::TestCase
   def test_exp_basic
      assert_respond_to(Math, :exp)
      assert_nothing_raised{ Math.exp(1) }
      assert_nothing_raised{ Math.exp(100) }
      assert_kind_of(Float, Math.exp(1))
   end
   
   def test_exp_positive
      assert_nothing_raised{ Math.exp(1) }
      assert_in_delta(2.71, Math.exp(1), 0.01)
   end
   
   def test_exp_zero
      assert_nothing_raised{ Math.exp(0) }
      assert_in_delta(1.0, Math.exp(0), 0.01)
   end
   
   def test_exp_negative
      assert_nothing_raised{ Math.exp(-1) }
      assert_in_delta(0.36, Math.exp(-1), 0.01)
   end
   
   def test_exp_positive_float
      assert_nothing_raised{ Math.exp(0.345) }
      assert_in_delta(1.41, Math.exp(0.345), 0.01)
   end
   
   def test_exp_negative_float
      assert_nothing_raised{ Math.exp(-0.345) }
      assert_in_delta(0.70, Math.exp(-0.345), 0.01)
   end
end
