#####################################################################
# tc_strip_bang.rb
#
# Test case for the String#strip! instance method. The tests for
# the String#strip method can be found in tc_strip.rb
#####################################################################
require 'test/unit'

class TC_String_StripBang_Instance < Test::Unit::TestCase
   def setup
      @string1 = " hello\r\n"
      @string2 = "hello\t               "
      @string3 = "hello  \000"
      @string4 = "hello  \000world\000  "
   end

   def test_strip_basic
      assert_respond_to(@string1, :strip!)
      assert_nothing_raised{ @string1.strip! }
      assert_kind_of(String, @string2.strip!)
   end

   def test_strip_bang
      assert_equal('hello', @string1.strip!)
      assert_equal('hello', @string2.strip!)
      assert_equal('hello', @string3.strip!)
      assert_equal("hello  \000world\000", @string4.strip!)
   end
   
   def test_strip_bang_modifies_original_string
      assert_nothing_raised{ @string3.strip! }
      assert_equal("hello", @string3)
   end   
   
   def test_strip_bang_edge_cases
      assert_equal(nil, ''.strip!)
      assert_equal('', "\000\000".strip!)
   end   

   def test_strip_bang_expected_errors
      assert_raises(ArgumentError){ @string1.strip!('x') }
      assert_raises(TypeError){ @string1.freeze.strip! }
   end

   def teardown
      @string1 = nil
      @string2 = nil
      @string3 = nil
      @string4 = nil
   end
end
