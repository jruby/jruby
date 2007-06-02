###############################################################################
# tc_swapcase.rb
#
# Test case for the String#swapcase and String#swapcase! instance methods.
###############################################################################
require 'test/unit'

class TC_String_Swapcase_Instance < Test::Unit::TestCase
   def setup
      @string = "hello 123 WORLD"
   end

   def test_swapcase_basic
      assert_respond_to(@string, :swapcase)
      assert_respond_to(@string, :swapcase!)
      assert_nothing_raised{ @string.swapcase }
      assert_nothing_raised{ @string.swapcase! }
      assert_kind_of(String, @string.swapcase)
      assert_kind_of(String, @string.swapcase!)
   end

   def test_swapcase
      assert_equal("HELLO 123 world", @string.swapcase)
      assert_equal("hello 123 WORLD", @string) # Object not modified
   end

   def test_swapcase_bang
      assert_equal("HELLO 123 world", @string.swapcase!)
      assert_equal("HELLO 123 world", @string) # Object modified
   end

   def test_swapcase_expected_errors
      assert_raises(ArgumentError){ @string.swapcase(1) }
   end

   def teardown
      @string = nil
   end
end
