###########################################################
# tc_each.rb
#
# Test suite for the Array#each instance method.
###########################################################
require "test/unit"

class TC_Array_Each_Instance < Test::Unit::TestCase
   def setup
      @array = ["ant", "bat", "cat", "dog"]
   end

   def test_each_basic
      assert_respond_to(@array, :each)
      assert_nothing_raised{ @array.each{} }
   end

   def test_each_iterate
      i = 0
      @array.each{ |e|
         assert_equal(@array[i], e)
         i += 1
      }
      assert_equal(4, i)
   end

   def test_each_noop_on_empty
      i = 0
      [].each{ i += 1 }
      assert_equal(0, i)
      assert_equal(@array, @array.each{})
   end

   def test_each_expected_errors
      assert_raises(ArgumentError){ @array.each(1){} }
   # No longer a valid test in 1.8.7
=begin
      assert_raises(LocalJumpError){ @array.each }
=end
   end

   def teardown
      @array = nil
   end
end
