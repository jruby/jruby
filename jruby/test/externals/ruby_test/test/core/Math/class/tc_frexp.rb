#####################################################################
# tc_frexp.rb
#
# Test cases for the Math.frexp method.
#####################################################################
require 'test/unit'

class TC_Math_Frexp_Class < Test::Unit::TestCase
   def test_frexp_basic
      assert_respond_to(Math, :frexp)
      assert_nothing_raised{ Math.frexp(1) }
      assert_nothing_raised{ Math.frexp(-100) }
      assert_kind_of(Array, Math.frexp(1234))
   end
  
   def test_frexp_positive
      assert_nothing_raised{ Math.frexp(1) }
      assert_in_delta(0.602, Math.frexp(1234).first, 0.01)
      assert_equal(11, Math.frexp(1234).last)
   end
   
   def test_frexp_zero
      assert_nothing_raised{ Math.frexp(0) }
      assert_in_delta(0.0, Math.frexp(0).first, 0.01)
      assert_equal(0, Math.frexp(0).last)
   end
    
   def test_frexp_negative
      assert_nothing_raised{ Math.frexp(-1) }
      assert_in_delta(-0.5, Math.frexp(-1).first, 0.01)
      assert_equal(1, Math.frexp(-1).last) # TODO: check
   end
   
   def test_frexp_positive_float
      assert_nothing_raised{ Math.frexp(0.345) }
      assert_in_delta(0.69, Math.frexp(0.345).first, 0.01)
      assert_equal(-1, Math.frexp(0.345).last) # TODO: check
   end
   
   def test_frexp_negative_float
      assert_nothing_raised{ Math.frexp(-0.345) }
      assert_in_delta(-0.69, Math.frexp(-0.345).first, 0.01)
      assert_equal(-1, Math.frexp(-0.345).last) # TODO: check
   end
end
