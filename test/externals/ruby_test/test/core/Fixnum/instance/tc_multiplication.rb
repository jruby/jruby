#####################################################################
# tc_multiplication.rb
#
# Test case for the Fixnum#* instance method.
#####################################################################
require 'test/unit'

class TC_Fixnum_Multiplication_InstanceMethod < Test::Unit::TestCase
   def setup
      @num_pos = 7
      @big_pos = 9223372036854775808
      @big_neg = -9223372036854775808
   end

   def test_multiplication_basic
      assert_respond_to(@num_pos, :*)
      assert_nothing_raised{ @num_pos.*(1) }
      assert_nothing_raised{ @num_pos * 1 }
   end

   def test_multiplication
      assert_equal(@num_pos, @num_pos * 1)
      assert_equal(42, @num_pos * 2 * 3)
      assert_equal(-42, @num_pos * 2 * -3)
      assert_equal(42, @num_pos * -2 * -3)
   end

   def test_multiplication_bignums
      assert_equal(true, @num_pos * 1 == @num_pos)
      assert_equal(true, @big_pos * 1 == @big_pos)
      assert_equal(true, @big_neg * 1 == @big_neg)
   end

   def test_multiplication_zero
      assert_equal(0, 0 * 0)
      assert_equal(0, 1 * 0)
      assert_equal(0, @big_pos * 0)
      assert_equal(0, @big_neg * 0)
   end

   def test_multiplication_expected_errors
      assert_raise(ArgumentError){ @num_pos.send(:*, 1, 2) }
   end

   def teardown
      @num_pos = nil
      @big_pos = nil
      @big_neg = nil
   end
end
