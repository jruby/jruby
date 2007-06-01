############################################################
# tc_to_s.rb
#
# Test suite for the Hash#to_s instance method.
############################################################
require "test/unit"

class TC_Hash_ToS_Instance < Test::Unit::TestCase
   def setup
      @hash = {"a", 1, :b, 2, nil, 3, false, 4}
   end

   def test_to_s_basic
      assert_respond_to(@hash, :to_s)
      assert_nothing_raised{ @hash.to_s }
   end

   def test_to_s
      assert_equal("a1b23false4".length, @hash.to_s.length)
      assert_equal("", {}.to_s)
      assert_equal("1", {nil,1}.to_s)
      assert_equal("false1", {false, 1}.to_s)
   end

   def test_to_s_expected_errors
      assert_raises(ArgumentError){ @hash.to_s(1) }
   end

   def teardown
      @hash = nil
   end
end
