#####################################################################
# tc_replace.rb
#
# Test suite for the Hash#replace instance method.
#
# TODO: Add $SAFE checks for $SAFE >= 4 (if possible).
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_Hash_Replace_InstanceMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @hash1  = {1,2,3,4}
      @hash2  = @hash1
      @frozen = {'a', 1, 'b', 2}.freeze
   end

   def test_replace_basic
      assert_respond_to(@hash1, :replace)
      assert_nothing_raised{ @hash1.replace({}) }
   end

   def test_replace
      assert_equal({1,2,3,4}, @hash1.replace({1,2,3,4}))
      assert_equal({1,2,3,4}, @hash1)
      assert_equal(@hash2, @hash1)
      assert_equal(@hash2.object_id, @hash1.object_id)
   end

   # Hash#replace is illegal in $SAFE mode 4 or higher
   unless JRUBY
      def test_replace_in_safe_mode
         assert_nothing_raised{
            proc do
               $SAFE = 3
               @hash1.replace({'a', 'b'})
            end.call
         }

         assert_raise(SecurityError){
            proc do
               $SAFE = 4
               @hash1.replace({'a', 'b'})
            end.call
         }
      end
   end

   # I would think calling replace on a frozen hash would raise a RuntimeError
   # not a TypeError, but there you go.
   #
   def test_replace_expected_errors
      assert_raise(ArgumentError){ @hash1.replace({}, {}) }
      assert_raise(TypeError){ @hash1.replace("test") }
      assert_raise(TypeError){ @frozen.replace(@hash1) }
   end

   def teardown
      @hash1  = nil
      @hash2  = nil
      @frozen = nil
   end
end
