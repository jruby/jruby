############################################################
# tc_invert.rb
#
# Test suite for the Hash#invert instance method.
############################################################
require "test/unit"

class TC_Hash_Invert_Instance < Test::Unit::TestCase
   def setup
      @hash = {:foo, 1, "bar", 2, nil, 3, false, 4}
   end

   def test_invert_basic
      assert_respond_to(@hash, :invert)
      assert_nothing_raised{ @hash.invert }
   end

   def test_invert
      assert_equal({1, :foo, 2, "bar", 3, nil, 4, false}, @hash.invert)
      assert_equal({:foo, 1, "bar", 2, nil, 3, false, 4}, @hash)
      assert_equal({}, {}.invert)
      assert_equal(1, {"a",1,"b",1,"c",1}.invert.size)
   end

   def test_expected_errors
      assert_raises(ArgumentError){ @hash.invert(1) }
   end

   def teardown
      @hash = nil
   end
end
