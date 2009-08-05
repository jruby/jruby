###########################################################
# tc_each_key.rb
#
# Test suite for the Hash#each_key instance method.
###########################################################
require "test/unit"

class TC_Hash_EachKey_Instance < Test::Unit::TestCase
   def setup
      @hash = {"ant", 1, "bat", 2, "cat", 3, "dog", 4}
   end

   def test_each_basic
      assert_respond_to(@hash, :each_key)
      assert_nothing_raised{ @hash.each_key{} }
   end

   def test_each_iterate
      i = 0
      @hash.each_key{ |key|
         assert_equal(true, ["ant","bat","cat","dog"].include?(key))
         i += 1
      }
      assert_equal(4, i)
   end

   def test_each_noop_on_empty
      i = 0
      {}.each_key{ i += 1 }
      assert_equal(0, i)
      assert_equal(@hash, @hash.each_key{})
   end

   def test_each_expected_errors
      assert_raises(ArgumentError){ @hash.each_key(1){} }
   # No longer a valid test in 1.8.7
=begin
      assert_raises(LocalJumpError){ @hash.each_key }
=end
   end

   def teardown
      @hash = nil
   end
end
