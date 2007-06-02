###########################################################
# tc_rassoc.rb
#
# Test suite for the Array#rassoc instance method.
###########################################################
require "test/unit"

class TC_Array_RAssoc_Instance < Test::Unit::TestCase
   def setup
      @array = [[false,"one"], [2,"two"], [nil,"three"], ["ii","two"], [1,nil]]
   end

   def test_rassoc_basic
      assert_respond_to(@array, :rassoc)
      assert_nothing_raised{ @array.rassoc("two") }
   end

   def test_rassoc
      assert_equal([2,"two"], @array.rassoc("two"))
      assert_equal([false,"one"], @array.rassoc("one"))
      assert_equal([nil,"three"], @array.rassoc("three"))
      assert_equal([1,nil], @array.rassoc(nil))
   end

   def test_rassoc_expected_errors
      assert_raises(ArgumentError){ @array.rassoc }
      assert_raises(ArgumentError){ @array.rassoc(1,2) }
   end

   def teardown
      @array = nil
   end
end
