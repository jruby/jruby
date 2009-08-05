##############################################################################
# tc_each.rb
#
# Test suite for the Hash#each instance method and the Hash#each_pair alias.
##############################################################################
require "test/unit"

class TC_Hash_Each_Instance < Test::Unit::TestCase
   def setup
      @hash = {"ant", 1, "bat", 2, "cat", 3, "dog", 4}
      @int  = 0
   end

   def test_each_basic
      assert_respond_to(@hash, :each)
      assert_nothing_raised{ @hash.each{} }
   end

   def test_each_pair_basic
      assert_respond_to(@hash, :each_pair)
      assert_nothing_raised{ @hash.each_pair{} }
   end

   def test_each_iterate
      @hash.each{ |key, value|
         assert_equal(value, @hash.delete(key))
         @int += 1
      }
      assert_equal(4, @int)
   end

   def test_each_pair_iterate
      @hash.each_pair{ |key, value|
         assert_equal(value, @hash.delete(key))
         @int += 1
      }
      assert_equal(4, @int)
   end

   def test_each_noop_on_empty
      {}.each{ @int += 1 }
      assert_equal(0, @int)
      assert_equal(@hash, @hash.each{})
   end

   def test_each_pair_noop_on_empty
      {}.each_pair{ @int += 1 }
      assert_equal(0, @int)
      assert_equal(@hash, @hash.each_pair{})
   end

   def test_each_expected_errors
      assert_raises(ArgumentError){ @hash.each(1){} }
   # No longer a valid test in 1.8.7
=begin
      assert_raises(LocalJumpError){ @hash.each }
=end
   end

   def test_each_pair_expected_errors
      assert_raises(ArgumentError){ @hash.each_pair(1){} }
   # No longer a valid test in 1.8.7
=begin
      assert_raises(LocalJumpError){ @hash.each_pair }
=end
   end

   def teardown
      @hash = nil
      @int  = nil
   end
end
