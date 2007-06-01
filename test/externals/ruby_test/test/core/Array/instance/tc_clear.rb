##########################################################
# tc_clear.rb
#
# Test suite for the Array#clear instance method.
##########################################################
require "test/unit"

class TC_Array_Clear_Instance < Test::Unit::TestCase
   def setup
      @array = [1,2,3]
   end

   def test_clear_method_basic
      assert_respond_to(@array, :clear)
      assert_nothing_raised{ @array.clear }
   end

   def test_clear_return_values
      assert_equal([], @array.clear)
      assert_equal([], [[], [], []].clear)
   end

   def teardown
      @array = nil
   end
end
