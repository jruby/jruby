###########################################################
# tc_sort.rb
#
# Test suite for the Hash#sort instance method.
###########################################################
require "test/unit"

class TC_Hash_Sort_Instance < Test::Unit::TestCase
   def setup
      @hash = {"c",3,"a",1,"b",2}
   end

   def test_sort_basic
      assert_respond_to(@hash, :sort)
      assert_nothing_raised{ @hash.sort }
      assert_nothing_raised{ @hash.sort{ |a,b| a <=> b } }
   end

   def test_sort
      assert_equal([["a",1],["b",2],["c",3]], @hash.sort)
      assert_equal([], {}.sort)
   end

   def test_sort_with_block
      assert_equal([["c",3],["b",2],["a",1]], @hash.sort{ |a,b| b <=> a })
      assert_equal([["c",3],["b",2],["a",1]], @hash.sort{ 1 } )
      assert_equal([], {}.sort{ |a,b| b <=> a })
   end

   def test_sort_expected_errors
      assert_raises(ArgumentError){ @hash.sort{} }
      assert_raises(NoMethodError){ @hash.sort{ true } }
   end

   def teardown
      @hash = nil
   end
end
