###########################################################
# tc_delete_at.rb
#
# Test suite for the Array#delete_at instance method.
###########################################################
require "test/unit"

class TC_Array_DeleteAt_Instance < Test::Unit::TestCase
   def setup
      @array = [1,2,3]
   end

   def test_delete_at_basic
      assert_respond_to(@array, :delete_at)
      assert_nothing_raised{ @array.delete_at(0) }
   end

   def test_delete_at_return_values
      assert_equal(2, @array.delete_at(1))
      assert_equal(nil, @array.delete_at(5))
      assert_equal(nil, @array.delete_at(-5))
   end

   def test_delete_at_modifies_receiver
      assert_nothing_raised{ @array.delete_at(1) }
      assert_equal([1,3], @array)

      assert_nothing_raised{ @array.delete_at(99) }
      assert_equal([1,3], @array)
   end

   def test_delete_at_expected_errors
      assert_raises(ArgumentError){ @array.delete_at }
      assert_raises(TypeError){ @array.delete_at("foo") }
   end

   def teardown
      @array = nil
   end
end
