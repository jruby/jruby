########################################################
# tc_sorted_set.rb
#
# Test case for the SortedSet class.
########################################################
require 'set'
require 'test/unit'

class TC_SortedSet_Stdlib < Test::Unit::TestCase
   def setup
      @set1 = SortedSet.new([7, 3, 5, 1, 9])
      @set2 = SortedSet.new([-7, -3, -5, -1, -9])
      @set3 = SortedSet.new(['1.8.2', '1.8.5', '1.8.3', '1.8.4'])
   end

   def test_new_aref_basic
      assert_nothing_raised{ SortedSet[] }
      assert_nothing_raised{ SortedSet[nil] }
      assert_nothing_raised{ SortedSet[[3,1,2]] }
   end

   def test_new_aref_values
      assert_equal(0, SortedSet[].size)
      assert_equal(1, SortedSet[nil].size)
      assert_equal(1, SortedSet[[]].size)
      assert_equal(1, SortedSet[[nil]].size)
   end

   def test_sorted_array
      assert_equal([1, 3, 5, 7, 9], @set1.to_a)
      assert_equal([-9, -7, -5, -3, -1], @set2.to_a)
   end

   def test_new_expected_errors
      assert_raises(ArgumentError){ SortedSet.new(false) }
      assert_raises(ArgumentError){ SortedSet.new(1) }
      assert_raises(ArgumentError){ SortedSet.new(1,2) }
   end

   def teardown
      @set1 = nil
      @set2 = nil
   end
end
