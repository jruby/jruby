############################################################################
# tc_length.rb
#
# Test suite for the Hash#length instance method and the Hash#size alias.
############################################################################
require "test/unit"

class TC_Hash_Length_InstanceMethod < Test::Unit::TestCase
   def setup
      @hash = {"a", 1, "b", 2, "c", 3}
   end

   def test_length_basic
      assert_respond_to(@hash, :length)
      assert_nothing_raised{ @hash.length }
   end
   
   def test_size_alias_basic
      assert_respond_to(@hash, :size)
      assert_nothing_raised{ @hash.size }
   end

   def test_length
      assert_equal(3, @hash.length)
      assert_equal(1, {"a",1,"a",2}.length)
      assert_equal(0, {}.length)
   end
   
   def test_length_edge_cases
      assert_equal(1, {'a', 1, 'a', 2, 'a', 3}.length)
      assert_equal(1, {nil,nil}.length)
      assert_equal(1, {true, true, true, true}.length)
   end
   
   def test_size_alias
      assert_equal(3, @hash.size)
      assert_equal(1, {"a",1,"a",2}.size)
      assert_equal(0, {}.size)
   end
   
   def test_size_alias_edge_cases
      assert_equal(1, {'a', 1, 'a', 2, 'a', 3}.size)
      assert_equal(1, {nil, nil}.size)
      assert_equal(1, {true, true, true, true}.size)
   end

   def test_length_expected_errors
      assert_raises(ArgumentError){ @hash.length(1) }
   end
   
   def test_size_alias_expected_errors
      assert_raises(ArgumentError){ @hash.size(1) }
   end

   def teardown
      @hash = nil
   end
end
