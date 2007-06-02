###########################################################
# tc_each.rb
#
# Test suite for the Hash#each instance method.
###########################################################
require "test/unit"

class TC_Hash_Each_Instance < Test::Unit::TestCase
   def setup
      @hash = {"ant", 1, "bat", 2, "cat", 3, "dog", 4}
   end

   def test_each_basic
      assert_respond_to(@hash, :each)
      assert_nothing_raised{ @hash.each{} }
   end

   def test_each_iterate
      i = 0
      @hash.each{ |key, value|
         assert_equal(value, @hash.delete(key))
         i += 1
      }
      assert_equal(4, i)
   end

   def test_each_noop_on_empty
      i = 0
      {}.each{ i += 1 }
      assert_equal(0, i)
      assert_equal(@hash, @hash.each{})
   end

   def test_each_expected_errors
      assert_raises(ArgumentError){ @hash.each(1){} }
      assert_raises(LocalJumpError){ @hash.each }
   end

   def teardown
      @hash = nil
   end
end
