#############################################################################
# tc_equality.rb
#
# Test suite for Array#== instance method. I added a custom class to verify
# that its to_ary method is handled properly by the Array#== method.
#############################################################################
require 'test/unit'
require 'test/helper'

class TC_Array_Equality_Instance < Test::Unit::TestCase
   include Test::Helper

   class AEquality
      def to_ary
         [1,2,3]
      end
   end

   def setup
      @array_int1    = [1, 2, 3]
      @array_int2    = [1, 2, 3]
      @array_int3    = [3, 1, 2]
      @array_chr_int = ['1', '2', '3']
      @custom        = AEquality.new
   end

   def test_basic
      assert_respond_to(@array_int1, :==)
      assert_nothing_raised{ @array_int1 == @array_int2 }
      assert_kind_of(Boolean, @array_int1 == @array_int2)
   end

   def test_equality_success
      msg = "=> See RubyForge bug #11585"
      assert_equal(true, @array_int1 == @array_int2)
      # Not standard Ruby behavior
      #assert_equal(true, @array_int1 == @custom, msg)
      assert_equal(true, [1.1, 2.1] == [1.1, 2.1])
   end

   def test_equality_failure
      assert_equal(false, @array_int1 == @array_int3)
      assert_equal(false, @array_int1 == @array_chr_int)
      assert_equal(false, @array_int1 == [])
      assert_equal(false, @array_int1 == nil)
      assert_equal(false, @array_int1 == 0)
      assert_equal(false, [1.11, 2.1] == [1.1, 2.1])
   end

   def test_edge_cases
      assert_equal(false, [0] == 0)
      assert_equal(false, [nil] == nil)
      assert_equal(false, [true] == true)
      assert_equal(false, [false] == false)
   end

   def teardown
      @array_int1 = nil
      @array_int2 = nil
      @array_int3 = nil
      @array_chr_int = nil
      @custom = nil
   end
end
