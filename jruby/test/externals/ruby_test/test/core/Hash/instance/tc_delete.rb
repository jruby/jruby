###########################################################
# tc_delete.rb
#
# Test suite for the Hash#delete instance method.
###########################################################
require "test/unit"

class TC_Hash_Delete_Instance < Test::Unit::TestCase
   def setup
      @hash = {:a, 1, :b, 2, :c, 3, :d, 4}
   end

   def test_delete_basic
      assert_respond_to(@hash, :delete)
   end

   def test_delete
      assert_equal(1, @hash.delete(:a))
      assert_equal(nil, @hash.delete(:f))
      assert_equal(nil, @hash.delete(nil))
   end

   def test_delete_with_block
      assert_equal(1, @hash.delete(:a){ 99 })
      assert_equal(99, @hash.delete(:f){ 99 })
   end

   def test_delete_expected_errors
      assert_raises(ArgumentError){ @hash.delete(1,2) }
   end

   def teardown
      @hash = nil
   end
end
