###########################################################
# tc_reverse_each.rb
#
# Test suite for the Array#reverse_each instance method.
###########################################################
require "test/unit"

class TC_Array_ReverseEach_Instance < Test::Unit::TestCase
   def setup
      @array = ["ant", "bat", "cat", "dog"]
   end

   def test_reverse_each_basic
      assert_respond_to(@array, :reverse_each)
      assert_nothing_raised{ @array.reverse_each{} }
   end

   def test_reverse_each_iterate
      i = 3
      @array.reverse_each{ |e|
         assert_equal(@array[i], e)
         i -= 1
      }
      assert_equal(-1, i)
   end

   def test_reverse_each_noop_on_empty
      i = 0
      [].reverse_each{ i += 1 }
      assert_equal(0, i)
      assert_equal(@array, @array.reverse_each{})
   end

   def test_reverse_each_expected_errors
      assert_raises(ArgumentError){ @array.reverse_each(1){} }
   # No longer a valid test in 1.8.7
=begin
      assert_raises(LocalJumpError){ @array.reverse_each }
=end
   end

   def teardown
      @array = nil
   end
end
