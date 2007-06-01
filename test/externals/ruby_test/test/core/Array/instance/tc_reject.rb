###########################################################
# tc_reject!.rb
#
# Test suite for the Array#reject! instance method.
###########################################################
require "test/unit"

class TC_Array_Reject_Instance < Test::Unit::TestCase
   def setup
      @array = [1,2,3,4]
   end

   def test_reject_basic
      assert_respond_to(@array, :reject!)
      assert_nothing_raised{ @array.reject!{ } }
   end

   def test_reject_results
      assert_nothing_raised{ @array.reject!{ |x| x > 2 } }
      assert_equal([1,2], @array)
   end

   def test_reject_returns_nil_if_no_changes
      assert_equal(nil, @array.reject!{ |x| x > 5 })
   end

   def test_reject_edge_cases
      assert_nothing_raised{ @array.reject!{ true } }
      assert_equal([], @array)
   end

   def test_reject_expected_errors
      assert_raises(ArgumentError){ @array.reject!(1) }
   end

   def teardown
      @array = nil
   end
end
