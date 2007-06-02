######################################################################
# tc_ceil.rb
#
# Test case for the Numeric#ceil instance method.
######################################################################
require 'test/unit'

class TC_Numeric_Ceil_InstanceMethod < Test::Unit::TestCase
   def setup
      @num_zero  = 0
      @num_pos   = 100
      @num_neg   = -100
      @num_posf  = 34.56
      @num_negf  = -34.56
      @num_twop  = 2147483648
      @num_twon  = -2147483648
      @num_twopb = 9223372036854775808
      @num_twonb = -9223372036854775808
   end
   
   def test_ceil_basic
      assert_respond_to(@num_pos, :ceil)
      assert_nothing_raised{ @num_pos.ceil }
   end
   
   def test_ceil_integer
      assert_equal(0, @num_zero.ceil)
      assert_equal(100, @num_pos.ceil)
      assert_equal(-100, @num_neg.ceil)
   end

   def test_ceil_float
      assert_equal(35, @num_posf.ceil)
      assert_equal(-34, @num_negf.ceil)
   end

   def test_ceil_twos_complement
      assert_equal(2147483648, @num_twop.ceil)
      assert_equal(-2147483648, @num_twon.ceil)
      assert_equal(9223372036854775808, @num_twopb.ceil)
      assert_equal(-9223372036854775808, @num_twonb.ceil)
   end
   
   def teardown
      @num_zero  = nil
      @num_pos   = nil
      @num_neg   = nil
      @num_posf  = nil
      @num_negf  = nil
      @num_twop  = nil
      @num_twon  = nil
      @num_twopb = nil
      @num_twonb = nil
   end
end