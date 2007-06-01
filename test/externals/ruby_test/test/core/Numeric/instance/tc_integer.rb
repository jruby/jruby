#####################################################################
# tc_integer.rb
#
# Test case for the Numeric#integer? instance method.
#####################################################################
require 'test/unit'

class TC_Numeric_Integer_InstanceMethod < Test::Unit::TestCase
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
   
   def test_integer_basic
      assert_respond_to(@num_pos, :integer?)
      assert_nothing_raised{ @num_pos.integer? }
   end
   
   def test_integer
      assert_equal(true, @num_zero.integer?)
      assert_equal(true, @num_pos.integer?)
      assert_equal(true, @num_neg.integer?)
      assert_equal(false, @num_posf.integer?)
      assert_equal(false, @num_negf.integer?)
      assert_equal(true, @num_twop.integer?)
      assert_equal(true, @num_twon.integer?)
      assert_equal(true, @num_twopb.integer?)
      assert_equal(true, @num_twonb.integer?)
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
