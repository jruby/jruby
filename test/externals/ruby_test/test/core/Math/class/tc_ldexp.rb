#####################################################################
# tc_ldexp.rb
#
# Test cases for the Math.ldexp method.
#####################################################################
require 'test/unit'

class TC_Math_Ldexp_Class < Test::Unit::TestCase
   def test_ldexp_basic
      assert_respond_to(Math, :ldexp)
      assert_nothing_raised{ Math.ldexp(1, 2) }
      assert_kind_of(Float, Math.ldexp(1, 2))
   end
   
   def test_ldexp_positive
      assert_nothing_raised{ Math.ldexp(1, 2) }
      assert_in_delta(4.0, Math.ldexp(1, 2), 0.01)
   end
   
   def test_ldexp_zero
      assert_nothing_raised{ Math.ldexp(0, 0) }
      assert_in_delta(0.0, Math.ldexp(0, 0), 0.01)
   end
   
   def test_ldexp_negative
      assert_nothing_raised{ Math.ldexp(-1, -2) }
      assert_in_delta(-0.25, Math.ldexp(-1, -2), 0.01)
   end
   
   def test_ldexp_positive_float
      assert_nothing_raised{ Math.ldexp(0.345, 0.655) }
      assert_in_delta(0.345, Math.ldexp(0.345, 0.655), 0.01)
   end
   
   def test_ldexp_negative_float
      assert_nothing_raised{ Math.ldexp(-0.345, -0.655) }
      assert_in_delta(-0.345, Math.ldexp(-0.345, -0.655), 0.01)
   end

   def test_ldexp_expected_errors
      assert_raises(ArgumentError){ Math.ldexp(0) }
      assert_raises(TypeError){ Math.ldexp(nil, nil) }
   end
end
