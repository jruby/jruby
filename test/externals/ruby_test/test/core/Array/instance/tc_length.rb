######################################################
# tc_length.rb
#
# Test for the Array#length instance method.
######################################################
require "test/unit"

class TC_Array_Length_Instance < Test::Unit::TestCase
   def setup
      @array = [1,2,[3,4]]
   end

   def test_length_basic
      assert_respond_to(@array, :length)
      assert_nothing_raised{ @array.length }
   end

   def test_length
      assert_equal(3, @array.length)
      assert_equal(0, [].length)
      assert_equal(1, [nil].length)
      assert_equal(1, [false].length)
   end

   def test_size_alias
      assert_respond_to(@array, :size)
      assert_equal(true, @array.method(:size) == @array.method(:length))
   end

   def test_length_expected_errors
      assert_raises(ArgumentError){ @array.length(1) }
   end

   def teardown
      @array = nil
   end
end
