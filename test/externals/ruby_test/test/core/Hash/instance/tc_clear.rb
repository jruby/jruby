###########################################################
# tc_clear.rb
#
# Test suite for the Hash#clear instance method.
###########################################################
require "test/unit"

class TC_Hash_Clear_InstanceMethod < Test::Unit::TestCase
   def setup
      @hash  = {"foo"=>1, :bar=>2}
      @empty = {}
   end

   def test_basic
      assert_respond_to(@hash, :clear)
      assert_nothing_raised{ @hash.clear }
   end

   def test_clear
      assert_equal({}, @hash.clear)
      assert_equal({}, {{:foo=>1,:bar=>2}=>3, {:baz=>4,:blah=>5}=>6}.clear)
   end

   def test_clear_empty
      assert_equal({}, @empty.clear)
      assert_equal(@empty.object_id, @empty.clear.object_id) # Not a dup
   end

   def test_expected_errors
      assert_raises(ArgumentError){ @hash.clear(1) }
   end

   def teardown
      @hash  = nil
      @empty = nil
   end
end
