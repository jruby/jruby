###########################################################
# tc_delete.rb
#
# Test suite for the Array#delete instance method.
###########################################################
require "test/unit"

class TC_Array_Delete_Instance < Test::Unit::TestCase
   def setup
      @array = [1, "two", nil, true, false, 3]
   end

   def test_delete_basic
      assert_respond_to(@array, :delete)
      assert_nothing_raised{ @array.delete(1) }
      assert_nothing_raised{ @array.delete(0){ 1 } }
   end

   def test_delete_results
      assert_equal(1, @array.delete(1))
      assert_equal("two", @array.delete("two"))
      assert_equal(nil, @array.delete(nil))
      assert_equal(true, @array.delete(true))
      assert_equal(false, @array.delete(false))
   end

   def test_delete_with_block
      assert_equal("failed", @array.delete(0){ "failed" })
      assert_equal(1, @array.delete(1){ "failed" })
   end

   def test_delete_modifies_receiver
      assert_nothing_raised{ @array.delete(1) }
      assert_equal(["two",nil,true,false,3], @array)
   end

   def test_expected_errors
      assert_raises(ArgumentError){ @array.delete }
      assert_raises(ArgumentError){ @array.delete(1,3) }
   end

   def teardown
      @array = nil
   end
end
