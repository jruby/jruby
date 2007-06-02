###########################################################
# tc_eql.rb
#
# Test for the Array#eql? instance method.
###########################################################
require "test/unit"

class TC_Array_Eql_Instance < Test::Unit::TestCase
   def setup
      @array1 = [1,2,3]
      @array2 = [1,2,3]
      @array3 = [3,2,1]
   end

   def test_eql_basic
      assert_respond_to(@array1, :eql?)
      assert_nothing_raised{ @array1.eql?(@array2) }
   end

   def test_eql_results
      assert_equal(true, @array1.eql?(@array1))
      assert_equal(true, @array1.eql?(@array2))
      assert_equal(false, @array1.eql?(@array3))
      assert_equal(false, @array1.eql?(nil))
      assert_equal(false, @array1.eql?(1))
   end

   def test_eql_expected_errors
      assert_raises(ArgumentError){ @array1.eql? }
      assert_raises(ArgumentError){ @array1.eql?([1],[2]) }
   end

   def teardown
      @array1 = nil
      @array2 = nil
      @array3 = nil
   end
end
