############################################################
# tc_keys.rb
#
# Test suite for the Hash#keys instance method.
############################################################
require "test/unit"

class TC_Hash_Keys_Instance < Test::Unit::TestCase
   def setup
      @hash = {"a", 1, "b", 2, "c", 3}
   end

   def test_keys_basic
      assert_respond_to(@hash, :keys)
      assert_nothing_raised{ @hash.keys }
   end

   def test_keys
      assert_equal(["a", "b", "c"], @hash.keys.sort)
      assert_equal(["a"], {"a",1,"a",2,"a",3}.keys)
      assert_equal([], {}.keys)
      assert_equal([nil], {nil,1}.keys)
   end

   def test_keys_expected_errors
      assert_raises(ArgumentError){ @hash.keys(1) }
   end

   def teardown
      @hash = nil
   end
end
