###############################################################################
# tc_to_s.rb
#
# Test case for the String#to_s instance method.
###############################################################################
require 'test/unit'

class TC_String_To_S_Instance < Test::Unit::TestCase
   def setup
      @string = "hello"
   end

   def test_to_s_basic
      assert_respond_to(@string, :to_s)
      assert_nothing_raised{ @string.to_s }
      assert_kind_of(String, @string.to_s)
   end

   def test_to_s
      assert_equal("hello", @string.to_s)
      assert_equal("nil", "nil".to_s)
      assert_equal("true", "true".to_s)
      assert_equal("false", "false".to_s)
      assert_equal("0", "0".to_s)
   end

   def test_to_s_expected_errors
      assert_raises(ArgumentError){ @string.to_s(1) }
   end

   def teardown
      @string = nil
   end
end
