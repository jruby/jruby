########################################################
# tc_set.rb
#
# Test case for the Set class.
########################################################
require "set"
require "test/unit"

class TC_Set_Stdlib < Test::Unit::TestCase
   def setup
      @set1 = Set.new([1,2,3])
      @set2 = Set.new([3,4,5])
   end

   # Test the Set[] style constructor
   def test_new_aref_basic
      assert_nothing_raised{ Set[] }
      assert_nothing_raised{ Set[nil] }
      assert_nothing_raised{ Set[[1,2,3]] }
   end

   def test_new_aref_values
      assert_equal(0, Set[].size)
      assert_equal(1, Set[nil].size)
      assert_equal(1, Set[[]].size)
      assert_equal(1, Set[[nil]].size)
   end

   def test_new_expected_errors
      assert_raises(ArgumentError){ Set.new(false) }
      assert_raises(ArgumentError){ Set.new(1) }
      assert_raises(ArgumentError){ Set.new(1,2) }
   end

   def teardown
      @set1 = nil
      @set2 = nil
   end
end
