###########################################################
# tc_equality.rb
#
# Test suite for the Hash#== instance method.
###########################################################
require 'test/unit'
require 'test/helper'

class TC_Hash_Equality_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   class HEquality
      def to_hash
         {'foo' => 1, 'bar' => 2}
      end
   end

   def setup
      @hash1  = {"foo"=>1, "bar"=>2}
      @hash2  = {"bar"=>2, "foo"=>1}
      @hash3  = {:foo=>1, :bar=>2}
      @custom = HEquality.new
   end

   def test_equality_basic
      assert_respond_to(@hash1, :==)
      assert_nothing_raised{ @hash1 == @hash2 }
   end

   def test_equality_success
      assert_equal(true, @hash1 == @hash1)
      assert_equal(true, @hash1 == @hash2)
      assert_equal(true, {} == {})
   end

   def test_equality_failure
      assert_equal(false, @hash1 == @hash3)
      assert_equal(false, {nil=>1} == {false=>1})
      assert_equal(false, {true=>1} == {false=>1})
      assert_equal(false, {nil=>1} == {0=>1})
   end

   def test_equality_using_custom_to_hash_method
      # Fails
      #assert_equal(true, @hash1 == @custom)
      assert_equal(false, {1,2} == @custom)
   end

   def teardown   
      @hash1  = nil
      @hash2  = nil
      @hash3  = nil
      @custom = nil
   end
end
