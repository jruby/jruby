###############################################################################
# tc_sort_by.rb
#
# Test case for the Enumerable#sort_by instance method.
###############################################################################
require 'test/unit'

class TC_Enumerable_SortBy_InstanceMethod < Test::Unit::TestCase
   def setup
      @words = ['apple', 'pear', 'fig']
      @nums  = [1, 0, -1, 77, 15]
      @mixed = [Time.now, 0, nil, true, false, 'hello']
   end

   def test_sort_by_basic
      assert_respond_to(@words, :sort_by)
      assert_nothing_raised{ @words.sort_by{ |w| w.length } }
   end

   def test_sort_by
      assert_equal(['fig', 'pear', 'apple'], @words.sort_by{ |w| w.length })
      assert_equal([0, 1, -1, 15, 77], @nums.sort_by{ |n| n.abs })
   end

   def test_sort_by_edge_cases
      assert_equal([], [].sort_by{ |n| n.to_s })
      assert_equal([nil, nil], [nil, nil].sort_by{ |n| n.to_s })
   end

   def test_sort_by_expected_errors
   # No longer a valid test in 1.8.7
=begin
      assert_raise(LocalJumpError){ @words.sort_by }
=end
      assert_raise(ArgumentError){ @words.sort_by(1) }
      assert_raise(NoMethodError){ @words.sort_by{} }
      assert_raise(NoMethodError){ @mixed.sort_by{ |m| m.length } }
   end

   def teardown
      @words = nil
      @nums  = nil
      @mixed = nil
   end
end
