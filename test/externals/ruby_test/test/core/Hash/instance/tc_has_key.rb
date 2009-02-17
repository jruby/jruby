#####################################################################
# tc_has_key.rb
#
# Test suite for the Hash#has_key? instance method as well as the
# Hash#include?, Hash#key? and Hash#member? aliases.
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_Hash_HasKey_Instance < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @hash = {:foo, 1, "bar", 2, nil, 3, false, 4}
   end

   def test_has_key_basic
      assert_respond_to(@hash, :has_key?)
      assert_nothing_raised{ @hash.has_key?(:foo) }
      assert_kind_of(Boolean, @hash.has_key?(1))
   end
   
   def test_key_basic
      assert_respond_to(@hash, :key?)
      assert_nothing_raised{ @hash.key?(:foo) }
      assert_kind_of(Boolean, @hash.key?(1))
   end
   
   def test_include_basic
      assert_respond_to(@hash, :has_key?)
      assert_nothing_raised{ @hash.include?(:foo) }
      assert_kind_of(Boolean, @hash.include?(1))
   end
     
   def test_member_basic
      assert_respond_to(@hash, :member?)
      assert_nothing_raised{ @hash.member?(:foo) }
      assert_kind_of(Boolean, @hash.member?(1))
   end

   def test_has_key_expected_true
      assert_equal(true, @hash.has_key?(:foo))
      assert_equal(true, @hash.has_key?("bar"))
      assert_equal(true, @hash.has_key?(nil))
      assert_equal(true, @hash.has_key?(false))
   end
   
   def test_key_expected_true
      assert_equal(true, @hash.key?(:foo))
      assert_equal(true, @hash.key?("bar"))
      assert_equal(true, @hash.key?(nil))
      assert_equal(true, @hash.key?(false))
   end
   
   def test_include_expected_true
      assert_equal(true, @hash.has_key?(:foo))
      assert_equal(true, @hash.has_key?("bar"))
      assert_equal(true, @hash.has_key?(nil))
      assert_equal(true, @hash.has_key?(false))
   end
      
   def test_member_expected_true
      assert_equal(true, @hash.member?(:foo))
      assert_equal(true, @hash.member?("bar"))
      assert_equal(true, @hash.member?(nil))
      assert_equal(true, @hash.member?(false))
   end
   
   def test_has_key_expected_false
      assert_equal(false, @hash.has_key?(99))
      assert_equal(false, @hash.has_key?(true))
      assert_equal(false, @hash.has_key?(1.0))
      assert_equal(false, @hash.has_key?(:bar))
   end
   
   def test_key_expected_false
      assert_equal(false, @hash.key?(99))
      assert_equal(false, @hash.key?(true))
      assert_equal(false, @hash.key?(1.0))
      assert_equal(false, @hash.key?(:bar))
   end
   
   def test_include_expected_false
      assert_equal(false, @hash.include?(99))
      assert_equal(false, @hash.include?(true))
      assert_equal(false, @hash.include?(1.0))
      assert_equal(false, @hash.include?(:bar))
   end
      
   def test_member_expected_false
      assert_equal(false, @hash.member?(99))
      assert_equal(false, @hash.member?(true))
      assert_equal(false, @hash.member?(1.0))
      assert_equal(false, @hash.member?(:bar))
   end

   def test_has_key_expected_errors
      assert_raises(ArgumentError){ @hash.has_key? }
      assert_raises(ArgumentError){ @hash.has_key?(1,2) }
   end
   
   def test_key_expected_errors
      assert_raises(ArgumentError){ @hash.key? }
      assert_raises(ArgumentError){ @hash.key?(1,2) }
   end
   
   def test_include_expected_errors
      assert_raises(ArgumentError){ @hash.include? }
      assert_raises(ArgumentError){ @hash.include?(1,2) }
   end
      
   def test_member_expected_errors
      assert_raises(ArgumentError){ @hash.member? }
      assert_raises(ArgumentError){ @hash.member?(1,2) }
   end

   def teardown
      @hash = nil
   end
end
