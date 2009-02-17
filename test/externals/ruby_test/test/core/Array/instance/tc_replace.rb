##############################################################################
# tc_replace.rb
#
# Test suite for the Array#replace instance method. Note that I've provided
# a custom class here to verify that Array#replace handles custom to_ary
# methods properly.
##############################################################################
require 'test/unit'
require 'test/helper'

class TC_Array_Replace_InstanceMethod < Test::Unit::TestCase
   include Test::Helper
   
   class AReplace
      def to_ary
         [1,2,3]
      end
   end

   def setup
      @array1 = %w/a b c d e/
      @array2 = @array1
      @custom = AReplace.new
      @frozen = [1, 2, 3].freeze
   end
   
   def test_replace_basic
      assert_respond_to(@array1, :replace)
      assert_nothing_raised{ @array1.replace([]) }
      assert_kind_of(Array, @array1.replace(@array2))
   end

   def test_replace
      assert_equal(["x", "y", "z"], @array1.replace(["x","y","z"]))
      assert_equal(["x", "y", "z"], @array1)
      assert_equal(@array2, @array1)
      assert_equal(@array2.object_id, @array1.object_id)
   end

   def test_replace_honors_to_ary
      assert_nothing_raised{ @array1.replace(@custom) }
      assert_equal([1,2,3], @array1)
   end

   # Array#replace is illegal in $SAFE level 4 or higher
   unless JRUBY
      def test_replace_in_safe_mode
         assert_nothing_raised{
            proc do
               $SAFE = 3
               @array1.replace(['a', 'b'])
            end.call
         }

         assert_raise(SecurityError){
            proc do
               $SAFE = 4
               @array1.replace(['a', 'b'])
            end.call
         }
      end
   end

   def test_replace_expected_errors
      assert_raise(ArgumentError){ @array1.replace("x","y") }
      assert_raise(TypeError){ @array1.replace("x") }
      assert_raise(TypeError){ @frozen.replace([4, 5, 6]) }
   end

   def teardown
      @array1 = nil
      @array2 = nil
      @custom = nil
      @frozen = nil
   end
end
