############################################################
# tc_to_a.rb
#
# Test suite for the Hash#to_a instance method.
############################################################
require "test/unit"

class TC_Hash_ToA_InstanceMethod < Test::Unit::TestCase
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
   
   def test_to_a_edge_cases
      assert_equal([['a', 3]], {'a', 1, 'a', 2, 'a', 3}.to_a)
      assert_equal([[nil, nil]], {nil, nil, nil, nil}.to_a)
      assert_equal([['a', [1,2]], ['b', [3,4]]], {'a', [1,2], 'b', [3,4]}.to_a)
   end

   def test_to_a_expected_errors
      assert_raises(ArgumentError){ @hash.to_a(1) }
   end

   def teardown
      @hash = nil
   end
end
