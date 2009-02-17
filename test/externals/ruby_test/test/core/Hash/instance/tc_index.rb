############################################################
# tc_index.rb
#
# Test suite for the Hash#index instance method.
############################################################
require 'test/unit'

class TC_Hash_Index_InstanceMethod < Test::Unit::TestCase
   def setup
      @hash = {:foo, 1, 'bar', 2, nil, 3, false, 4, 'baz', 1, 3.007, 6.934}
   end

   def test_index_basic
      assert_respond_to(@hash, :index)
      assert_nothing_raised{ @hash.index(1) }
   end

   def test_index
      assert_equal('bar', @hash.index(2))
      assert_equal(nil, @hash.index(3))
      assert_equal(false, @hash.index(4))
      assert_equal(nil, @hash.index(99))
      assert_equal(3.007, @hash.index(6.934))
   end

   def test_index_multiple_entries
      assert_equal(true, [:foo, 'baz'].include?(@hash.index(1)))
   end

   def test_index_expected_errors
      assert_raise(ArgumentError){ @hash.index }
   end

   def teardown
      @hash = nil
   end
end
