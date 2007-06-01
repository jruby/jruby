############################################################
# tc_rehash.rb
#
# Test suite for the Hash#rehash instance method.
############################################################
require "test/unit"

class TC_Hash_Rehash_Instance < Test::Unit::TestCase
   def setup
      @array = ['a', 'b']
      @hash  = {@array, 100}
   end

   def test_rehash_basic
      assert_respond_to(@hash, :rehash)
      assert_nothing_raised{ @hash.rehash }
   end

   def test_rehash
      assert_equal(100, @hash[@array])          # Check index
      assert_equal({['a', 'b'] => 100}, @hash)  # Check hash itself

      assert_nothing_raised{ @array[0] = 'c' }  # Change the object
      assert_equal(nil, @hash[@array])          # Nil until rehashed

      assert_nothing_raised{ @hash.rehash }     # Rehash the hash
      assert_equal({['c','b'] => 100}, @hash)   # Check hash again
   end

   # Try to break it using nils within objects
   def test_rehash_edge_cases
      obj = [nil, nil]

      assert_nothing_raised{ @hash = {obj => 100} }
      assert_equal(100, @hash[obj])
      assert_equal({[nil,nil] => 100}, @hash)

      obj[0] = 0

      assert_equal(nil, @hash[obj])
      assert_nothing_raised{ @hash.rehash }
      assert_equal({[0, nil] => 100}, @hash)
   end

   def teardown
      @array = nil
      @hash  = nil
   end
end
