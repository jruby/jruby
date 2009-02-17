######################################################################
# tc_any.rb
#
# Test case for the Enumerable#any? instance method.
######################################################################
require 'test/unit'

class TC_Enumerable_Any_InstanceMethod < Test::Unit::TestCase
   def setup
      @enum = ['a', 'b', 'c']
   end

   def test_any_basic
      assert_respond_to(@enum, :any?)
      assert_nothing_raised{ @enum.any? }
      assert_nothing_raised{ @enum.any?{ } }
   end

   def test_any_no_block
      assert_equal(true, [1, 2, 3].any?)
      assert_equal(true, [nil, false, true].any?)
      assert_equal(false, [nil, false].any?)
   end

   def test_any_with_block
      assert_equal(true, [1, 2, 3].any?{ |e| e > 1 })
      assert_equal(false, [1, 2, 3].any?{ |e| e > 7 })
   end

   def test_any_with_explicit_false_and_nil
      assert_equal(true, [false, nil].any?{ |e| e.nil? })
      assert_equal(true, [false, nil].any?{ |e| e == false })
   end

   def test_any_edge_cases
      assert_equal(false, [].any?)
      assert_equal(true, [0].any?)
      assert_equal(true, [true].any?)
   end

   def test_any_expected_errors
      assert_raise(ArgumentError){ [1, 2, 3].any?(1) }
   end

   def teardown
      @enum = nil
   end
end
