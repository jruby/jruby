############################################################################
# tc_upcase_bang.rb
#
# Test case for the String#upcase! instance method. Tests for the
# String#upcase method can be found in the tc_upcase.rb file.
#
# TODO: Add extended ASCII tests (?)
############################################################################
require 'test/unit'

class TC_String_UpcaseBang_InstanceMethod < Test::Unit::TestCase
   def setup
      @str = '<html><b>hello</b></html>'
   end

   def test_upcase_basic
      assert_respond_to(@str, :upcase)
      assert_nothing_raised{ @str.upcase! }
      assert_kind_of(String, 'hello'.upcase!)
   end

   def test_upcase
      assert_equal('<HTML><B>HELLO</B></HTML>', @str.upcase!)
      assert_equal(nil, 'HELLO'.upcase!)
   end

   def test_upcase_non_alpha
      assert_equal(nil, '123'.upcase!)
      assert_equal(nil, '!@#$%^&*()'.upcase!)
   end

   def test_upcase_original_modified
      @str = 'hello'
      assert_nothing_raised{ @str.upcase! }
      assert_equal('HELLO', @str)
   end

   def test_upcase_edge_cases
      assert_equal(nil, ''.upcase!)
      assert_equal(nil, ' '.upcase!)
      assert_equal(nil, "\000\000".upcase!)
   end

   def test_upcase_expected_errors
      assert_raise(ArgumentError){ @str.upcase!('test') }
      assert_raise(TypeError){ @str.freeze.upcase! }
   end

   def teardown
      @str = nil
   end
end
