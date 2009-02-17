######################################################################
# tc_all.rb
#
# Test case for the Enumerable#all? instance method.
######################################################################
require 'test/unit'

class TC_Enumerable_All_InstanceMethod < Test::Unit::TestCase
   def setup
      @enum = ['a', 'b', 'c']
   end

   def test_all_basic
      assert_respond_to(@enum, :all?)
      assert_nothing_raised{ @enum.all? }
      assert_nothing_raised{ @enum.all?{ } }
   end

   def test_all_no_block
      assert_equal(true, [1, 2, 3].all?)
      assert_equal(false, [nil, false, true].all?)
      assert_equal(false, [nil, false].all?)
   end

   def test_all_with_block
      assert_equal(false, [1, 2, 3].all?{ |e| e > 1 })
      assert_equal(true, [1, 2, 3].all?{ |e| e > 0 })
   end

   def test_all_with_explicit_false_and_nil
      assert_equal(true, [nil].all?{ |e| e.nil? })
      assert_equal(true, [false].all?{ |e| e == false })
   end

   def test_all_edge_cases
      assert_equal(true, [].all?)
      assert_equal(true, [0].all?)
      assert_equal(true, [true].all?)
   end

   def test_all_expected_errors
      assert_raise(ArgumentError){ [1, 2, 3].all?(1) }
   end

   def teardown
      @enum = nil
   end
end
