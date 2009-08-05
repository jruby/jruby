###########################################################
# tc_delete_if.rb
#
# Test suite for the Hash#delete_if instance method.
###########################################################
require "test/unit"

class TC_Hash_DeleteIf_Instance < Test::Unit::TestCase
   def setup
      @hash = {:foo, 1, "bar", 2, nil, 3, false, 4}
   end

   def test_delete_if_basic
      assert_respond_to(@hash, :delete_if)
      assert_nothing_raised{ @hash.delete_if{} }
   end

   def test_delete_if
      assert_equal({:foo, 1, "bar", 2}, @hash.delete_if{ |k,v| v > 2 })
      assert_equal({:foo, 1, "bar", 2}, @hash.delete_if{ |k,v| v > 5 })
      assert_equal({}, @hash.delete_if{ |k,v| v >= 0 })
   end

   def test_delete_if_expected_errors
   # No longer a valid test in 1.8.7
=begin
      assert_raises(LocalJumpError){ @hash.delete_if }
=end
      assert_raises(ArgumentError){ @hash.delete_if(1){} }
   end

   def teardown
      @hash = nil
   end
end
