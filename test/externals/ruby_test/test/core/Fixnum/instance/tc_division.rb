#####################################################################
# tc_division.rb
#
# Test case for the Fixnum#/ instance method.
#####################################################################
require 'test/unit'

class TC_Fixnum_Division_InstanceMethod < Test::Unit::TestCase
   def setup
      @num_pos = 42
      @big_pos = 9223372036854775808
      @big_neg = -9223372036854775808
   end

   def test_division_basic
      assert_respond_to(@num_pos, :/)
      assert_nothing_raised{ @num_pos./(1) }
      assert_nothing_raised{ @num_pos / 1 }
   end

   def test_division
      assert_equal(@num_pos, @num_pos / 1)
      assert_equal(7, @num_pos / 2 / 3)
      assert_equal(-7, @num_pos / 2 / -3)
      assert_equal(7, @num_pos / -2 / -3)
   end

   def test_division_bignums
      assert_equal(true, @num_pos / 1 == @num_pos)
      assert_equal(true, @big_pos / 1 == @big_pos)
      assert_equal(true, @big_neg / 1 == @big_neg)
   end

   def test_division_one
      assert_equal(0, 0 / 1)
      assert_equal(1, 1 / 1)
      assert_equal(@big_pos, @big_pos / 1)
      assert_equal(@big_neg, @big_neg / 1)
   end

   def test_division_expected_errors
      assert_raise(ArgumentError){ @num_pos.send(:/, 1, 2) }
      assert_raise(ZeroDivisionError){ @num_pos / 0 }
   end

   def teardown
      @num_pos = nil
      @big_pos = nil
      @big_neg = nil
   end
end
