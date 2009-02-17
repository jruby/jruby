########################################################################
# tc_capitalize.rb
#
# Test suite for the String#capitalize instance method. Tests for the
# String#capitalize! method are in tc_capitalize_bang.rb.
#
# TODO: Add extended ASCII tests (?)
########################################################################
require 'test/unit'

class TC_String_Capitalize_InstanceMethod < Test::Unit::TestCase
   def setup
      @string_basic   = "hello"
      @string_numbers = "123"
      @string_mixed   = "123heLLo"
      @string_capped  = "HELLO"
      @string_empty   = ""
   end

   def test_capitalize_basic
      assert_respond_to(@string_basic, :capitalize)
      assert_nothing_raised{ @string_basic.capitalize }
      assert_kind_of(String, @string_basic.capitalize)
   end

   def test_capitalize
      assert_equal("Hello", @string_basic.capitalize)
      assert_equal("123", @string_numbers.capitalize)
      assert_equal("123hello", @string_mixed.capitalize)
      assert_equal("Hello", @string_capped.capitalize)
      assert_equal("", @string_empty.capitalize)
   end

   def test_capitalize_edge_cases
      assert_equal('', ''.capitalize)
      assert_equal(' ', ' '.capitalize)
      assert_equal("\000\000", "\000\000".capitalize)
   end

   def test_capitalize_expected_errors
      assert_raise(ArgumentError){ @string_basic.capitalize("bogus") }
   end
   
   def teardown
      @string_basic   = nil
      @string_numbers = nil
      @string_mixed   = nil
      @string_capped  = nil
      @string_empty   = nil
   end
end
