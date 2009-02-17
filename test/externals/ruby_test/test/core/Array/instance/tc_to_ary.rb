###########################################################
# tc_to_ary.rb
#
# Test suite for the Array#to_ary instance method. We also
# validate return values for arrays with a custom to_ary
# method, and those that have no to_ary method.
###########################################################
require "test/unit"

class TC_Array_ToAry_Instance < Test::Unit::TestCase
   class ToAryArray1 < Array
      def to_ary
         'hello'
      end
   end

   class ToAryArray2 < Array
      undef_method :to_ary
   end

   def setup
      @array  = [1,2,3]
      @nested = ['a', 1, ['b', 2, ['c', 3, 4]]]
      @redef  = ToAryArray1[1, 2, 3]
      @undef  = ToAryArray2[1, 2, 3]
   end

   def test_to_ary_basic
      assert_respond_to(@array, :to_ary)
      assert_nothing_raised{ @array.to_ary }
   end
   
   def test_to_ary
      assert_equal(@array, @array.to_ary)
      assert_equal(@array.object_id, @array.to_ary.object_id)
   end

   def test_to_ary_nested
      assert_equal(@nested, @nested.to_ary)
      assert_equal(@nested.object_id, @nested.to_ary.object_id)
   end

   def test_to_ary_custom
      assert_equal('hello', @redef.to_ary)
   end

   def test_to_ary_undefined
      assert_raise(NoMethodError){ @undef.to_ary }
   end

   def test_expected_errors
      assert_raises(ArgumentError){ @array.to_ary(1) }
   end

   def teardown
      @array  = nil
      @nested = nil
      @redef  = nil
      @undef  = nil
   end
end
