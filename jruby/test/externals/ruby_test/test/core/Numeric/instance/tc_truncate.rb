######################################################################
# tc_truncate.rb
#
# Test case for the Numeric#truncate instance method.
######################################################################
require 'test/unit'

class TC_Numeric_Truncate_InstanceMethod < Test::Unit::TestCase
   def setup
      @num_zero  = 0.0
      @num_pos   = 100
      @num_neg   = -100
      @num_posf  = 34.56
      @num_negf  = -34.56
      @num_twop  = 2147483648
      @num_twon  = -2147483648
      @num_twopb = 9223372036854775808.9223372036854775808
      @num_twonb = -9223372036854775808.000000000000000000
   end

   def test_truncate_basic
      assert_respond_to(@num_pos, :truncate)
      assert_nothing_raised{ @num_pos.truncate }
   end

   def test_truncate_integer
      assert_equal(0, @num_zero.truncate)
      assert_equal(100, @num_pos.truncate)
      assert_equal(-100, @num_neg.truncate)
   end

   def test_truncate_float
      assert_equal(34, @num_posf.truncate)
      assert_equal(-34, @num_negf.truncate)
   end

   def test_truncate_twos_complement
      assert_equal(2147483648, @num_twop.truncate)
      assert_equal(-2147483648, @num_twon.truncate)
      assert_equal(9223372036854775808, @num_twopb.truncate)
      assert_equal(-9223372036854775808, @num_twonb.truncate)
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
