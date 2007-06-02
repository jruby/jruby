###########################################################
# tc_values_at.rb
#
# Test suite for the Array#values_at instance method.
###########################################################
require "test/unit"

class TC_Array_ValuesAt_Instance < Test::Unit::TestCase
   def setup
      @array = %w/a b c d e f/
   end

   def test_basic
      assert_respond_to(@array, :values_at)
      assert_nothing_raised{ @array.values_at }
   end

   def test_values_at
      assert_equal(["b","d","f"], @array.values_at(1,3,5))
      assert_equal(["b","d","f"], @array.values_at(1.0,3.0,5.0))
      assert_equal(["b","d","f",nil], @array.values_at(1,3,5,7))
      assert_equal(["f","d","b",nil], @array.values_at(-1,-3,-5,-7))
      assert_equal(["b","c","d","c","d","e"], @array.values_at(1..3, 2...5))
      assert_equal([nil], @array.values_at(99))
   end

   def test_expected_errors
      assert_raises(TypeError){ @array.values_at(nil) }
      assert_raises(TypeError){ @array.values_at([]) }
      assert_raises(TypeError){ @array.values_at("foo") }
      assert_raises(TypeError){ @array.values_at(true) }
   end

   def teardown
      @array = nil
   end
end
