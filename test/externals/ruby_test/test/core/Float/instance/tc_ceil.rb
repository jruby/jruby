######################################################################
# tc_ceil.rb
#
# Test case for the Float#ceil instance method.
######################################################################
require 'test/unit'

class Test_Float_Ceil_InstanceMethod < Test::Unit::TestCase
   def setup
      @float_pos = 1.07
      @float_neg = -0.93
   end

   def test_ceil_basic
      assert_respond_to(@float_pos, :ceil)
      assert_nothing_raised{ @float_pos.ceil }
      assert_kind_of(Integer, @float_pos.ceil)
   end

   def test_ceil
      assert_equal(2, @float_pos.ceil)
      assert_equal(0, @float_neg.ceil)
      assert_equal(1, (1.0).ceil)
      assert_equal(0, (0.0).ceil)
   end

   def test_ceil_expected_errors
      assert_raises(ArgumentError){ @float_pos.ceil(1) }
   end

   def teardown
      @float_pos = nil
      @float_neg = nil
   end
end
