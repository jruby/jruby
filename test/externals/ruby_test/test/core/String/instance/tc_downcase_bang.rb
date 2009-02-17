############################################################################
# tc_downcase_bang_bang.rb
#
# Test case for the String#downcase! instance method. Tests for the
# String#downcase instance method can be found in tc_downcase.rb.
#
# TODO: Add Unicode tests.
############################################################################
require 'test/unit'

class TC_String_DowncaseBang_InstanceMethod < Test::Unit::TestCase
   def setup
      @str = '<HTML><B>HELLO</B></HTML>'
   end

   def test_downcase_bang_basic
      assert_respond_to(@str, :downcase)
      assert_nothing_raised{ @str.downcase! }
      assert_kind_of(String, 'HELLO'.downcase!)
   end

   def test_downcase
      assert_equal('<html><b>hello</b></html>', @str.downcase!)
      assert_equal(nil, 'hello'.downcase!)
   end

   def test_downcase_bang_non_alpha
      assert_equal(nil, '123'.downcase!)
      assert_equal(nil, '!@#$%^&*()'.downcase!)
   end

   def test_downcase_bang_original_modified
      @str = 'HELLO'
      assert_nothing_raised{ @str.downcase! }
      assert_equal('hello', @str)
   end

   def test_downcase_bang_edge_cases
      assert_equal(nil, ''.downcase!)
      assert_equal(nil, ' '.downcase!)
      assert_equal(nil, "\000\000".downcase!)
   end

   def test_downcase_bang_expected_errors
      assert_raise(ArgumentError){ @str.downcase!('test') }
      assert_raise(TypeError){ @str.freeze.downcase! }
   end

   def teardown
      @str = nil
   end
end
