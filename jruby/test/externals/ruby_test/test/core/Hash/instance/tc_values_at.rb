###########################################################
# tc_values_at.rb
#
# Test suite for the Hash#values_at instance method.
###########################################################
require "test/unit"

class TC_Hash_ValuesAt_Instance < Test::Unit::TestCase
   def setup
      @hash1 = {"a",1,"b",2,"c",3}
      @hash2 = {"a",1,"b",2,"c",3}
      @hash3 = Hash.new{ |hash, key| hash[key] = "test" }

      @hash2.default = 7
   end

   def test_values_at_basic
      assert_respond_to(@hash1, :values_at)
      assert_nothing_raised{ @hash1.values_at("a") }
   end

   def test_values_at
      assert_equal([1], @hash1.values_at("a"))
      assert_equal([1,nil], @hash1.values_at("a","z"))
      assert_equal([1,2,3], @hash1.values_at("a","b","c"))
      assert_equal([nil], @hash1.values_at("z"))
   end

   def test_values_at_default
      assert_equal([1], @hash2.values_at("a"))
      assert_equal([1,7], @hash2.values_at("a","z"))
      assert_equal([1,2,3], @hash2.values_at("a","b","c"))
      assert_equal([7], @hash2.values_at("z"))
   end

   def test_values_at_block
      assert_equal(["test"], @hash3.values_at("z"))
      assert_equal(["test", "test"], @hash3.values_at("x","z"))
   end

   def teardown
      @hash1 = nil
      @hash2 = nil
      @hash3 = nil
   end
end
