#####################################################################
# tc_replace.rb
#
# Test suite for the Hash#replace instance method.
#####################################################################
require "test/unit"

class TC_Hash_Replace_Instance < Test::Unit::TestCase
   def setup
      @hash1 = {1,2,3,4}
      @hash2 = @hash1
   end

   def test_replace_basic
      assert_respond_to(@hash1, :replace)
      assert_nothing_raised{ @hash1.replace({}) }
   end

   def test_replace
      assert_equal({1,2,3,4}, @hash1.replace({1,2,3,4}))
      assert_equal({1,2,3,4}, @hash1)
      assert_equal(@hash2, @hash1)
      assert_equal(@hash2.object_id, @hash1.object_id)
   end

   def test_replace_expected_errors
      assert_raises(ArgumentError){ @hash1.replace({}, {}) }
      assert_raises(TypeError){ @hash1.replace("test") }
   end

   def teardown
      @hash1 = nil
      @hash2 = nil
   end
end
