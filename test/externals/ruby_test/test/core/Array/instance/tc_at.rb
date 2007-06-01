##########################################################
# tc_at.rb
#
# Test suite for the Array#at instance method.
##########################################################
require "test/unit"

class TC_Array_At_Instance < Test::Unit::TestCase
   def setup
      @array = [1, "two", true, false, nil, 9]
   end

   def test_at_method_basic
      assert_respond_to(@array, :at)
      assert_nothing_raised{ @array.at(0) }
   end

   def test_at_method_results
      assert_equal(1, @array.at(0))
      assert_equal(9, @array.at(5))
      assert_equal(9, @array.at(-1))
      assert_equal(nil, @array.at(99))
      assert_equal(nil, @array.at(-99))
   end

   def test_expected_errors
      assert_raises(ArgumentError){ @array.at }
      assert_raises(TypeError){ @array.at(nil) }
      assert_raises(TypeError){ @array.at("a") }
   end

   def teardown
      @array = nil
   end
end
