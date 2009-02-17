######################################################
# tc_nitems.rb
#
# Test suite for the Array#nitems instance method.
######################################################
require "test/unit"

class TC_Array_NItems_Instance < Test::Unit::TestCase
   def setup
      @array  = [1,nil,"two",nil,3]
      @nested = [1, nil, [2, nil], 3, [nil, 4]]
   end

   def test_nitems_basic
      assert_respond_to(@array, :nitems)
      assert_nothing_raised{ @array.nitems }
   end

   def test_nitems
      assert_equal(3, @array.nitems)
      assert_equal(0, [nil,nil].nitems)
      assert_equal(1, [false,nil].nitems)
   end

   def test_nitems_nested
      assert_equal(4, @nested.nitems)
   end

   def test_nitems_expected_errors
      assert_raises(ArgumentError){ @array.nitems(1) }
   end

   def teardown
      @array  = nil
      @nested = nil
   end
end
