###########################################################
# tc_clear.rb
#
# Test suite for the Hash#clear instance method.
###########################################################
require "test/unit"

class TC_Hash_Clear_Instance < Test::Unit::TestCase
   def setup
      @hash = {"foo"=>1, :bar=>2}
   end

   def test_basic
      assert_respond_to(@hash, :clear)
      assert_nothing_raised{ @hash.clear }
   end

   def test_clear
      assert_equal({}, @hash.clear)
      assert_equal({}, {{:foo=>1,:bar=>2}=>3, {:baz=>4,:blah=>5}=>6}.clear)
   end

   def test_expected_errors
      assert_raises(ArgumentError){ @hash.clear(1) }
   end

   def teardown
      @hash = nil
   end
end
