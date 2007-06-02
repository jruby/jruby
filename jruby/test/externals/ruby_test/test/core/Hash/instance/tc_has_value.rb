############################################################
# tc_has_value.rb
#
# Test suite for the Hash#has_value? instance method.
############################################################
require "test/unit"

class TC_Hash_HasValue_Instance < Test::Unit::TestCase
   def setup
      @hash = {:foo, 1, "bar", 2, nil, 3, false, 4}
   end

   def test_has_value_basic
      assert_respond_to(@hash, :has_value?)
      assert_nothing_raised{ @hash.has_value?(1) }
   end

   def test_has_value
      assert_equal(true, @hash.has_value?(1))
      assert_equal(true, @hash.has_value?(2))
      assert_equal(true, @hash.has_value?(3))
      assert_equal(true, @hash.has_value?(4))

      assert_equal(false, @hash.has_value?(99))
      assert_equal(false, @hash.has_value?(false))
      assert_equal(false, @hash.has_value?(nil))
   end

   def test_has_value_expected_errors
      assert_raises(ArgumentError){ @hash.has_value? }
   end

   def teardown
      @hash = nil
   end
end
