######################################################
# tc_include.rb
#
# Test suite for the Array#include? method.
######################################################
require "test/unit"

class TC_Array_Include_Instance < Test::Unit::TestCase
   def setup
      @array = ["one", 2, nil, false, true, 3]
   end

   def test_include_basic
      assert_respond_to(@array, :include?)
      assert_nothing_raised{ @array.include?(2) }
   end

   def test_include
      assert_equal(true, @array.include?("one"))
      assert_equal(true, @array.include?(2))
      assert_equal(true, @array.include?(nil))
      assert_equal(true, @array.include?(false))
      assert_equal(false, @array.include?("2"))
   end

   def test_include_expected_errors
      assert_raises(ArgumentError){ @array.include?(1,2) }
   end

   def teardown
      @array = nil
   end
end
