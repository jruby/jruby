############################################################
# tc_length.rb
#
# Test suite for the Hash#length instance method.
############################################################
require "test/unit"

class TC_Hash_Length_Instance < Test::Unit::TestCase
   def setup
      @hash = {"a", 1, "b", 2, "c", 3}
   end

   def test_length_basic
      assert_respond_to(@hash, :length)
      assert_nothing_raised{ @hash.length }
   end

   def test_length
      assert_equal(3, @hash.length)
      assert_equal(1, {"a",1,"a",2}.length)
      assert_equal(0, {}.length)
      assert_equal(1, {nil,nil}.length)
   end

   def test_length_expected_errors
      assert_raises(ArgumentError){ @hash.length(1) }
   end

   def teardown
      @hash = nil
   end
end
