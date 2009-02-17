############################################################################
# tc_upcase.rb
#
# Test case for the String#upcase instance method. Tests for the
# String#upcase! method can be found in the tc_upcase_bang.rb file.
#
# TODO: Add extended ASCII tests (?)
############################################################################
require 'test/unit'

class TC_String_Upcase_InstanceMethod < Test::Unit::TestCase
   def setup
      @str = '<html><b>hello</b></html>'
   end

   def test_upcase_basic
      assert_respond_to(@str, :upcase)
      assert_nothing_raised{ @str.upcase }
      assert_kind_of(String, @str.upcase)
   end

   def test_upcase
      assert_equal('<HTML><B>HELLO</B></HTML>', @str.upcase)
      assert_equal('HELLO', 'HELLO'.upcase)
   end

   def test_upcase_non_alpha
      assert_equal('123', '123'.upcase)
      assert_equal('!@#$%^&*()', '!@#$%^&*()'.upcase)
   end

   def test_upcase_original_unmodified
      @str = 'hello'
      assert_nothing_raised{ @str.upcase }
      assert_equal('hello', @str)
   end

   def test_upcase_frozen_string_allowed
      @str = 'hello'
      assert_equal('HELLO', @str.freeze.upcase)
   end

   def test_upcase_edge_cases
      assert_equal('', ''.upcase)
      assert_equal(' ', ' '.upcase)
      assert_equal("\000\000", "\000\000".upcase)
   end

   def test_upcase_expected_errors
      assert_raise(ArgumentError){ @str.upcase('test') }
   end

   def teardown
      @str = nil
   end
end
