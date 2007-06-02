###########################################################
# tc_delete_if.rb
#
# Test suite for the Array#delete_if instance method.
###########################################################
require "test/unit"

class TC_Array_DeleteIf_Instance < Test::Unit::TestCase
   def setup
      @array = [1,2,3,4]
   end

   def test_delete_if_basic
      assert_respond_to(@array, :delete_if)
      assert_nothing_raised{ @array.delete_if{ } }
   end

   def test_delete_if_results
      assert_nothing_raised{ @array.delete_if{ |x| x > 2 } }
      assert_equal([1,2], @array)
   end

   def test_delete_returns_array_if_no_changes
      assert_equal([1,2,3,4], @array.delete_if{ |x| x > 5 })
   end

   def test_delete_if_edge_cases
      assert_nothing_raised{ @array.delete_if{ true } }
      assert_equal([], @array)
   end

   def test_delete_if_expected_errors
      assert_raises(ArgumentError){ @array.delete_if(1) }
   end

   def teardown
      @array = nil
   end
end
