###########################################################
# tc_reverse.rb
#
# Test suite for the Array#reverse and Array#reverse!
# instance methods.
###########################################################
require "test/unit"

class TC_Array_Reverse_Instance < Test::Unit::TestCase
   def setup
      @array = [1,2,3]
   end

   def test_reverse_basic
      assert_respond_to(@array, :reverse)
      assert_nothing_raised{ @array.reverse }
      assert_nothing_raised{ @array.reverse! }
   end

   def test_reverse
      assert_equal([3,2,1], @array.reverse)
      assert_equal([nil,false,nil], [nil,false,nil].reverse)
   end

   def test_reverse_bang
      assert_equal([3,2,1], @array.reverse!)
      assert_equal([3,2,1], @array)
   end

   def test_reverse_expected_errors
      assert_raises(ArgumentError){ @array.reverse(1) }
      assert_raises(ArgumentError){ @array.reverse!(1) }
   end

   def teardown
      @array = nil
   end
end
