###########################################################
# tc_empty.rb
#
# Test suite for the Hash#empty? instance method.
###########################################################
require "test/unit"

class TC_Hash_Empty_Instance < Test::Unit::TestCase
   def setup
      @hash1 = {nil, nil}
      @hash2 = {}
   end

   def test_empty_basic
      assert_respond_to(@hash1, :empty?)
      assert_nothing_raised{ @hash1.empty? }
   end

   def test_empty
      assert_equal(false, @hash1.empty?)
      assert_equal(true, @hash2.empty?)
   end

   def test_empty_expected_errors
      assert_raises(ArgumentError){ @hash1.empty?(1) }
   end

   def teardown
      @hash1 = nil
      @hash2 = nil
   end
end
