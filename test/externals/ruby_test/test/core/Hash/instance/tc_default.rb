######################################################################
# tc_default.rb
#
# Test suite for the Hash#default and Hash#default= instance methods.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_Hash_Default_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @hash1 = Hash.new
      @hash2 = Hash.new("test")
      @hash3 = Hash.new{ |h,k| h[k] = k.to_i * 3 }
   end

   def test_default_basic
      assert_respond_to(@hash1, :default)
      assert_respond_to(@hash1, :default=)
      assert_nothing_raised{ @hash1.default }
      assert_nothing_raised{ @hash1.default=0 }
   end

   # The default value for Hash.new{} was changed in Ruby 1.8.5
   def test_default_get
      assert_equal(nil, @hash1.default)
      assert_equal("test", @hash2.default)
      assert_equal(0, @hash3.default(nil))
      assert_equal(0, @hash3.default(0))
      assert_equal(9, @hash3.default(3))
      
      if RELEASE > 4
         assert_equal(nil, @hash3.default)
      else
         assert_equal(0, @hash3.default)
      end
   end

   def test_default_set
      assert_nothing_raised{ @hash3.default = "foo" }
      assert_equal('foo', @hash3.default)
      assert_equal('foo', @hash3.default(3))
      assert_equal('foo', @hash3.default('x'))
   end

   def test_default_with_key
      assert_equal(nil, @hash1.default(2))
      assert_equal("test", @hash2.default(2))
      assert_equal(6, @hash3.default(2))
   end

   def test_default_expected_errors
      assert_raises(ArgumentError){ @hash1.default(1,2) }
   end

   def teardown
      @hash1 = nil   
      @hash2 = nil
      @hash3 = nil
   end
end
