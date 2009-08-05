###########################################################
# tc_each_value.rb
#
# Test suite for the Hash#each_value instance method.
###########################################################
require "test/unit"

class TC_Hash_EachValue_Instance < Test::Unit::TestCase
   def setup
      @hash = {"ant", 1, "bat", 2, "cat", 3, "dog", 4}
   end

   def test_each_basic
      assert_respond_to(@hash, :each_value)
      assert_nothing_raised{ @hash.each_value{} }
   end

   def test_each_iterate
      i = 0
      @hash.each_value{ |key|
         assert_equal(true, [1,2,3,4].include?(key))
         i += 1
      }
      assert_equal(4, i)
   end

   def test_each_noop_on_empty
      i = 0
      {}.each_value{ i += 1 }
      assert_equal(0, i)
      assert_equal(@hash, @hash.each_value{})
   end

   def test_each_expected_errors
      assert_raises(ArgumentError){ @hash.each_value(1){} }
   # No longer a valid test in 1.8.7
=begin
      assert_raises(LocalJumpError){ @hash.each_value }
=end
   end

   def teardown
      @hash = nil
   end
end
