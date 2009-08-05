###########################################################
# tc_each_index.rb
#
# Test suite for the Array#each_index instance method.
###########################################################
require "test/unit"

class TC_Array_EachIndex_Instance < Test::Unit::TestCase
   def setup
      @array = %w/ant bat cat dog/
   end

   def test_each_index_basic
      assert_respond_to(@array, :each_index)
      assert_nothing_raised{ @array.each_index{} }
   end

   def test_each_index_behavior
      i = 0
      @array.each_index{ |index|
         assert_equal(i, index)
         i += 1
      }
      assert_equal(4, i)
      assert_equal(@array, @array.each_index{}) 
   end

   def test_each_index_expected_errors
      assert_raises(ArgumentError){ @array.each_index(1){} }
   # No longer a valid test in 1.8.7
=begin
      assert_raises(LocalJumpError){ @array.each_index }
=end
   end

   def teardown
      @array = nil
   end
end
