###############################################################################
# tc_hash.rb
#
# Test case for the Array#hash instance method. We test this separately from
# the Object#hash method because the Array class has a custom implementation
# in array.c.
###############################################################################
require 'test/unit'
require 'test/helper'

class TC_Array_Hash_InstanceMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup   
      @array_chr = ['a', 'b', 'c']
      @array_int = [1, 2, 3]
      @array_mix = ['a', 1, 3.5]
   end

   def test_hash_basic
      assert_respond_to(@array_chr, :hash)
      assert_nothing_raised{ @array_chr.hash }
      assert_kind_of(Fixnum, @array_chr.hash)
   end

   # There's no way to verify the exact hash value of a float because it's
   # platform dependent.
   #
   def test_hash
      assert_equal(292, @array_chr.hash)
      
      if JRUBY
         assert_equal(27, @array_int.hash)
      else
         assert_equal(25, @array_int.hash)
      end
   end
   
   def test_hash_equality
      assert_equal(true, [1,2,3].hash == [1,2,3].hash)
      assert_equal(true, ['a','b','c'].hash == ['a','b','c'].hash)
   end

   def test_hash_expected_errors
      assert_raise(ArgumentError){ @array_chr.hash(1) }
   end

   def teardown
      @array_chr = nil
      @array_int = nil
      @array_mix = nil
   end
end
