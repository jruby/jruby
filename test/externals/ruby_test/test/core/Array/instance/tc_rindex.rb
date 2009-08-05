######################################################
# tc_rindex.rb
#
# Test suite for the Array#rindex instance method.
######################################################
require "test/unit"

class TC_Array_RIndex_Instance < Test::Unit::TestCase
   def setup   
      @array = [1,"two",nil,false,true,"two",nil]
   end

   def test_rindex_basic
      assert_respond_to(@array, :rindex)
      assert_nothing_raised{ @array.rindex(1) }
   end

   def test_rindex
      assert_equal(0, @array.rindex(1))
      assert_equal(5, @array.rindex("two"))
      assert_equal(6, @array.rindex(nil))
      assert_equal(3, @array.rindex(false))
      assert_equal(4, @array.rindex(true))
      assert_equal(nil, @array.rindex(99))
   end

   def test_rindex_expected_errors
   # No longer a valid test in 1.8.7
=begin
      assert_raises(ArgumentError){ @array.rindex }
      assert_raises(ArgumentError){ @array.rindex(0,1) }
=end
   end

   def teardown
      @array = nil
   end
end
