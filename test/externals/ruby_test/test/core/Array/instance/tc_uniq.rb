####################################################################
# tc_uniq.rb
#
# Test suite for the Array#uniq and Array#unique instance methods.
####################################################################
require "test/unit"

class TC_Array_Uniq_Instance < Test::Unit::TestCase
   def setup   
      @array = ["a","b","b","c","c","c",nil,nil,false,false,true,true]
   end

   def test_basic
      assert_respond_to(@array, :uniq)
      assert_respond_to(@array, :uniq!)
      assert_nothing_raised{ @array.uniq }
      assert_nothing_raised{ @array.uniq! }
   end

   def test_uniq
      assert_equal(["a","b","c",nil,false,true], @array.uniq)
      assert_equal([1,2,3], [1,2,3].uniq)
      assert_equal([nil], [nil].uniq)
      assert_equal(
         ["a","b","b","c","c","c",nil,nil,false,false,true,true], @array
      )
   end

   def test_uniq_bang
      assert_equal(["a","b","c",nil,false,true], @array.uniq!)
      assert_equal(["a","b","c",nil,false,true], @array)
      assert_equal(nil, [1,2,3].uniq!)
      assert_equal(nil, [nil].uniq!)
   end

   def test_expected_errors
      assert_raises(ArgumentError){ @array.uniq(1) }
      assert_raises(ArgumentError){ @array.uniq!(1) }
   end

   def teardown
      @array = nil
   end
end
