###########################################################
# tc_replace.rb
#
# Test suite for the Array#replace instance method.
###########################################################
require "test/unit"

class TC_Array_Replace_Instance < Test::Unit::TestCase
   def setup
      @array1 = %w/a b c d e/
      @array2 = @array1
   end
   
   def test_replace_basic
      assert_respond_to(@array1, :replace)
      assert_nothing_raised{ @array1.replace([]) }
   end

   def test_replace
      assert_equal(["x", "y", "z"], @array1.replace(["x","y","z"]))
      assert_equal(["x", "y", "z"], @array1)
      assert_equal(@array2, @array1)
      assert_equal(@array2.object_id, @array1.object_id)
   end

   def test_replace_expected_errors
      assert_raises(ArgumentError){ @array1.replace("x","y") }
      assert_raises(TypeError){ @array1.replace("x") }
   end

   def teardown
      @array1 = nil
      @array2 = nil
   end
end
