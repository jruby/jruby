#####################################################################
# tc_subtraction.rb
#
# Test case for the Fixnum#- instance method.
#####################################################################
require 'test/unit'

class TC_Fixnum_Subtraction_InstanceMethod < Test::Unit::TestCase
   def setup
      @num_pos = 7
      @big_pos = (2**63)
      @big_neg = -(2**63)
   end

   def test_subtraction_basic
      assert_respond_to(@num_pos, :-)
      assert_nothing_raised{ @num_pos.-(1) }
      assert_nothing_raised{ @num_pos - 1 }
   end

   def test_subtraction
      assert_equal(@num_pos, @num_pos - 0)
      assert_equal(2, @num_pos - 2 - 3)
      assert_equal(true, @num_pos - 1 < @num_pos)
      assert_equal(true, @num_pos - 0.1 < @num_pos)
   end

   def test_subtraction_zero
      assert_equal(0, 0 - 0)
      assert_equal(0, 1 - 1)
      assert_equal(0, -1 - (-1))
      assert_equal(0, @big_pos - @big_pos)
   end

   def test_subtraction_expected_errors
      assert_raise(ArgumentError){ @num_pos.send(:-, 1, 2) }
   end

   def teardown
      @num_pos = nil
      @big_pos = nil
      @big_neg = nil
   end
end
