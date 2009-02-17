############################################################################
# tc_downcase.rb
#
# Test case for the String#downcase instance method. Tests for the
# String#downcase! instance method can be found in tc_downcase_bang.rb.
#
# TODO: Add Unicode tests.
############################################################################
require 'test/unit'

class TC_String_Downcase_InstanceMethod < Test::Unit::TestCase
   def setup
      @str = '<HTML><B>HELLO</B></HTML>'
   end

   def test_downcase_basic
      assert_respond_to(@str, :downcase)
      assert_nothing_raised{ @str.downcase }
      assert_kind_of(String, @str.downcase)
   end

   def test_downcase
      assert_equal('<html><b>hello</b></html>', @str.downcase)
      assert_equal('hello', 'hello'.downcase)
   end

   def test_downcase_non_alpha
      assert_equal('123', '123'.downcase)
      assert_equal('!@#$%^&*()', '!@#$%^&*()'.downcase)
   end

   def test_downcase_original_unmodified
      @str = 'HELLO'
      assert_nothing_raised{ @str.downcase }
      assert_equal('HELLO', @str)
   end
   
   def test_downcase_frozen_string_allowed
      @str = 'HELLO'
      assert_equal('hello', @str.freeze.downcase) 
   end

   def test_downcase_edge_cases
      assert_equal('', ''.downcase)
      assert_equal(' ', ' '.downcase)
      assert_equal("\000\000", "\000\000".downcase)
   end

   def test_downcase_expected_errors
      assert_raise(ArgumentError){ @str.downcase('test') }
   end

   def teardown
      @str = nil
   end
end
