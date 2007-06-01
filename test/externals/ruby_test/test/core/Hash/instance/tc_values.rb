############################################################
# tc_values.rb
#
# Test suite for the Hash#values instance method.
############################################################
require "test/unit"

class TC_Hash_Values_Instance < Test::Unit::TestCase
   def setup
      @hash = {"a", 1, "b", 2, "c", 3}
   end

   def test_values_basic
      assert_respond_to(@hash, :values)
      assert_nothing_raised{ @hash.values }
   end

   def test_values
      assert_equal([1,2,3], @hash.values.sort)
      assert_equal([1,1,1], {"a",1,"b",1,"c",1}.values)
      assert_equal([], {}.values)
      assert_equal([nil], {1,nil}.values)
   end

   def test_values_expected_errors
      assert_raises(ArgumentError){ @hash.values(1) }
   end

   def teardown
      @hash = nil
   end
end
