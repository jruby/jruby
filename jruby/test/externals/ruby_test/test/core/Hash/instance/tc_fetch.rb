###########################################################
# tc_fetch.rb
#
# Test suite for the Hash#fetch instance method.
###########################################################
require "test/unit"

class TC_Hash_Fetch_Instance < Test::Unit::TestCase
   def setup
      @hash = {:foo, 1, "bar", 2, nil, 3, false, 4}
   end

   def test_fetch_basic
      assert_respond_to(@hash, :fetch)
      assert_nothing_raised{ @hash.fetch(:foo) }
      assert_nothing_raised{ @hash.fetch("test"){} }
   end

   def test_fetch
      assert_equal(1, @hash.fetch(:foo))
      assert_equal(2, @hash.fetch("bar"))
      assert_equal(3, @hash.fetch(nil))
      assert_equal(4, @hash.fetch(false))
   end

   def test_fetch_with_default_value
      assert_equal("test", @hash.fetch(99,"test"))
      assert_equal("test", @hash.fetch(true,"test"))
   end

   def test_fetch_with_block
      assert_equal("test", @hash.fetch(99){ "test" })
      assert_equal("test", @hash.fetch(true){ "test" })
   end

   def test_fetch_expected_errors
      assert_raises(IndexError){ @hash.fetch("bogus") }
      assert_raises(IndexError){ @hash.fetch(1) }
      assert_raises(ArgumentError){ @hash.fetch }
      assert_raises(ArgumentError){ @hash.fetch{} }
   end

   def teardown
      @hash = nil
   end
end
