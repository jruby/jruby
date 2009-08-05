###########################################################
# tc_select.rb
#
# Test suite for the Hash#select instance method.
###########################################################
require "test/unit"

class TC_Hash_Select_Instance < Test::Unit::TestCase
   def setup
      @hash = {"a",1,"b",2,"c",3}
   end

   def test_select_basic
      assert_respond_to(@hash, :select)
      assert_nothing_raised{ @hash.select{} }
   end

   def test_select
      assert_equal([["a",1],["b",2]], @hash.select{ |k,v| v <=2 })
      assert_equal([["a",1],["b",2],["c",3]], @hash.select{ true })
      assert_equal([], @hash.select{ false })
      assert_equal([], @hash.select{ |k,v| v > 99 })
   end

   def test_select_expected_errors
   # No longer a valid test in 1.8.7
=begin
      assert_raises(LocalJumpError){ @hash.select }
=end
      assert_raises(ArgumentError){ @hash.select(1) }
   end

   def teardown
      @hash = nil
   end
end
