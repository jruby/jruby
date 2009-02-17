###########################################################
# tc_sort.rb
#
# Test suite for the Hash#sort instance method.
###########################################################
require 'test/unit'
require 'test/helper'

class TC_Hash_Sort_InstanceMethod < Test::Unit::TestCase
   include Test::Helper
   
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

   # JRuby uses the MRI 1.9/2.0 hash sorting
   def test_sort_with_block
      assert_equal([["c",3],["b",2],["a",1]], @hash.sort{ |a,b| b <=> a })
      assert_equal([["c",3],["b",2],["a",1]], @hash.sort{ 1 } ) unless JRUBY
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
