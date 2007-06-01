############################################################
# tc_has_key.rb
#
# Test suite for the Hash#has_key? instance method.
############################################################
require "test/unit"

class TC_Hash_HasKey_Instance < Test::Unit::TestCase
   def setup
      @hash = {:foo, 1, "bar", 2, nil, 3, false, 4}
   end

   def test_has_key_basic
      assert_respond_to(@hash, :has_key?)
      assert_nothing_raised{ @hash.has_key?(:foo) }
   end

   def test_has_key
      assert_equal(true, @hash.has_key?(:foo))
      assert_equal(true, @hash.has_key?("bar"))
      assert_equal(true, @hash.has_key?(nil))
      assert_equal(true, @hash.has_key?(false))

      assert_equal(false, @hash.has_key?(99))
      assert_equal(false, @hash.has_key?(true))
   end

   def test_has_key_expected_errors
      assert_raises(ArgumentError){ @hash.has_key? }
   end

   def teardown
      @hash = nil
   end
end
