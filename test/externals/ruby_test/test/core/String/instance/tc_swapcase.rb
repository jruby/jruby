###############################################################################
# tc_swapcase.rb
#
# Test case for the String#swapcase instance method. Tests for the
# String#swapcase! method can be found in the tc_swapcase_bang.rb instance
# method.
#
# TODO: Add unicode tests.
###############################################################################
require 'test/unit'

class TC_String_Swapcase_InstanceMethod < Test::Unit::TestCase
   def setup
      @string = 'hello 123 WORLD'
   end

   def test_swapcase_basic
      assert_respond_to(@string, :swapcase)
      assert_nothing_raised{ @string.swapcase }
      assert_kind_of(String, @string.swapcase)
   end

   def test_swapcase
      assert_equal('HELLO 123 world', @string.swapcase)
   end

   def test_swapcase_frozen_object
      assert_nothing_raised{ @string.freeze.swapcase }
   end

   def test_swapcase_original_string_unmodified
      assert_nothing_raised{ @string.swapcase }
      assert_equal('hello 123 WORLD', @string)
   end

   def test_swapcase_edge_cases
      assert_equal('', ''.swapcase)
      assert_equal(' ', ' '.swapcase)
      assert_equal('123', '123'.swapcase)
      assert_equal('!@#$%^&*()', '!@#$%^&*()'.swapcase)
   end

   def test_swapcase_expected_errors
      assert_raise(ArgumentError){ @string.swapcase(1) }
   end

   def teardown
      @string = nil
   end
end
