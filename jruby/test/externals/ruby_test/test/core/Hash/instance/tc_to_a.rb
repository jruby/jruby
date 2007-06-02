############################################################
# tc_to_a.rb
#
# Test suite for the Hash#to_a instance method.
############################################################
require "test/unit"

class TC_Hash_ToA_Instance < Test::Unit::TestCase
   def setup
      @hash = {"a",1,"b",2,"c",3}
   end

   def test_to_a_basic
      assert_respond_to(@hash, :to_a)
      assert_nothing_raised{ @hash.to_a }
      assert_kind_of(Array, @hash.to_a)
   end

   def test_to_a
      assert_equal([["a",1],["b",2],["c",3]], @hash.to_a)
      assert_equal([], {}.to_a)
   end

   def test_to_a_expected_errors
      assert_raises(ArgumentError){ @hash.to_a(1) }
   end

   def teardown
      @hash = nil
   end
end
