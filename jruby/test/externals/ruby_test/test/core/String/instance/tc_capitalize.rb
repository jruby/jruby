########################################################################
# tc_capitalize.rb
#
# Test suite for the String#capitalize and String#capitalize! methods.
########################################################################
require "test/unit"

class TC_String_Capitalize_Instance < Test::Unit::TestCase
   def setup
      @string_basic   = "hello"
      @string_numbers = "123"
      @string_mixed   = "123heLLo"
      @string_capped  = "HELLO"
      @string_empty   = ""
   end

   def test_basic
      assert_respond_to(@string_basic, :capitalize)
      assert_nothing_raised{ @string_basic.capitalize }
   end

   def test_capitalize_results
      assert_equal("Hello", @string_basic.capitalize)
      assert_equal("123", @string_numbers.capitalize)
      assert_equal("123hello", @string_mixed.capitalize)
      assert_equal("Hello", @string_capped.capitalize)
      assert_equal("", @string_empty.capitalize)
   end

   def test_capitalize_bang_results
      assert_equal("Hello", @string_basic.capitalize!)
      assert_equal(nil, @string_numbers.capitalize!)
      assert_equal("123hello", @string_mixed.capitalize!)
      assert_equal("Hello", @string_capped.capitalize!)
      assert_equal(nil, @string_empty.capitalize!)
   end

   def test_basic_bang
      assert_respond_to(@string_basic, :capitalize!)
      assert_nothing_raised{ @string_basic.capitalize! }
   end

   def test_expected_errors
      assert_raises(ArgumentError){ @string_basic.capitalize("bogus") }
   end

   def teardown
      @string_basic   = nil
      @string_numbers = nil
      @string_mixed   = nil
      @string_capped  = nil
      @string_empty   = nil
   end
end
