###############################################################################
# tc_swapcase_bang.rb
#
# Test case for the String#swapcase! instance method. Tests for the
# String#swapcase method can be found in the tc_swapcase.rb instance
# method.
#
# TODO: Add unicode tests.
###############################################################################
require 'test/unit'

class TC_String_SwapcaseBang_InstanceMethod < Test::Unit::TestCase
   def setup
      @string = 'hello 123 WORLD'
   end

   def test_swapcase_basic
      assert_respond_to(@string, :swapcase)
      assert_nothing_raised{ @string.swapcase! }
      assert_kind_of(String, 'hello'.swapcase!)
   end

   def test_swapcase
      assert_equal('HELLO 123 world', @string.swapcase!)
      assert_equal('hello 123 WORLD', @string.swapcase!)
   end

   def test_swapcase_original_string_modified
      assert_nothing_raised{ @string.swapcase! }
      assert_equal('HELLO 123 world', @string)
   end

   def test_swapcase_edge_cases
      assert_equal(nil, ''.swapcase!)
      assert_equal(nil, ' '.swapcase!)
      assert_equal(nil, '123'.swapcase!)
      assert_equal(nil, '!@#$%^&*()'.swapcase!)
   end

   def test_swapcase_expected_errors
      assert_raise(ArgumentError){ @string.swapcase!(1) }
      assert_raise(TypeError){ @string.freeze.swapcase! }
   end

   def teardown
      @string = nil
   end
end
