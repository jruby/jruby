######################################################
# tc_index.rb
#
# Test suite for the Array#index instance method.
######################################################
require "test/unit"

class TC_Array_Index_Instance < Test::Unit::TestCase
   def setup   
      @array = [1,"two",nil,false,true]
   end

   def test_index_basic
      assert_respond_to(@array, :index)
      assert_nothing_raised{ @array.index(1) }
   end

   def test_index
      assert_equal(0, @array.index(1))
      assert_equal(1, @array.index("two"))
      assert_equal(2, @array.index(nil))
      assert_equal(3, @array.index(false))
      assert_equal(4, @array.index(true))
      assert_equal(nil, @array.index(99))
   end

   def test_index_expected_errors
   # No longer a valid test in 1.8.7
=begin
      assert_raises(ArgumentError){ @array.index }
      assert_raises(ArgumentError){ @array.index(0,1) }
=end
   end

   def teardown
      @array = nil
   end
end
