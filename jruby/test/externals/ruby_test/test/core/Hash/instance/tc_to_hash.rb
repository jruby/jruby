############################################################
# tc_to_hash.rb
#
# Test suite for the Hash#to_hash instance method.
############################################################
require "test/unit"

# Used to validate to_hash
class FooHash
   def to_hash
      {"a",1,"b",2}
   end
end

class TC_Hash_ToHash_Instance < Test::Unit::TestCase
   def setup
      @hash = {}
      @foo  = FooHash.new
   end

   def test_to_hash_basic
      assert_respond_to(@hash, :to_hash)
      assert_nothing_raised{ @hash.to_hash }
   end

   def test_to_hash
      assert_equal({}, @hash)
      assert_nothing_raised{ @hash.replace(@foo) }
      assert_equal({"a",1,"b",2}, @hash)
   end

   def test_to_hash_expected_errors
      assert_raises(TypeError){ @hash.replace(1) }
   end

   def teardown
      @hash = nil
      @foo  = nil
   end
end
