###########################################################
# tc_to_s.rb
#
# Test suite for the Array#to_s instance method.
###########################################################
require "test/unit"

class TC_Array_ToS_Instance < Test::Unit::TestCase
   def setup
      @array = [1,2,3]
   end

   def test_to_s_basic
      assert_respond_to(@array, :to_s)
      assert_nothing_raised{ @array.to_s }
   end

   def test_to_s
      assert_equal("123", @array.to_s)
      assert_equal("", [].to_s)
      assert_equal("", [nil].to_s)
      assert_equal("false", [false].to_s)
   end

   def test_expected_errors
      assert_raises(ArgumentError){ @array.to_s(1) }
   end

   def teardown
      @array = nil
   end
end
