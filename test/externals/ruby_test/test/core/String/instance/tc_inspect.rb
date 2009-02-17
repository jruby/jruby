########################################################################
# tc_inspect.rb
#
# Test case for the String#inspect instance method. This test suite
# was added because string.c has a custom implementation.
########################################################################
require 'test/unit'

class TC_String_Inspect_InstanceMethod < Test::Unit::TestCase
   def setup
      @string = 'hello'
      $string = 'world'
   end

   def test_inspect_basic
      assert_respond_to(@string, :inspect)
      assert_nothing_raised{ @string.inspect }
      assert_kind_of(String, @string.inspect)
   end

   def test_inspect
      assert_equal('"abc"', "abc".inspect)
      assert_equal('"123"', "123".inspect)
   end

   def test_inspect_literal_quotes
      assert_equal('"\'x\'"', "'x'".inspect)
      assert_equal('"\"x\""', "\"x\"".inspect)
   end

   def test_inspect_backslash
      assert_equal('"\\\\foo\\\\bar"', '\foo\bar'.inspect)
      assert_equal('"C:\\\\foo\\\\bar"', "C:\\foo\\bar".inspect)
   end

   # See the IS_EVSTR macro in string.c
   def test_inspect_special_characters
      string = 'hello'
      assert_equal('"$string"', "$string".inspect)
      assert_equal('"@string"', "@string".inspect)
      assert_equal('"#string"', "#string".inspect)
      assert_equal('"hello"', "#{string}".inspect)
      assert_equal('"world"', "#$string".inspect)
      assert_equal('"hello"', "#@string".inspect)
   end

   def test_inspect_with_newline
      assert_equal('"x\\ny\\n"', "x\ny\n".inspect)
      assert_equal('"x\\ry\\r"', "x\ry\r".inspect)
   end

   def test_inspect_with_tab
      assert_equal('"x\\ty\\t"', "x\ty\t".inspect)
   end

   def test_inspect_with_form_feed
      assert_equal('"x\\fy\\f"', "x\fy\f".inspect)
   end

   # These are specially handled in string.c
   def test_inspect_special_control_characters
      assert_equal('"x\\ay\\a"', "x\007y\007".inspect) # Beep
      assert_equal('"x\\by\\b"', "x\010y\010".inspect) # Backspace
      assert_equal('"x\\vy\\v"', "x\013y\013".inspect) # Vertical tab
      assert_equal('"x\\ey\\e"', "x\033y\033".inspect) # Escape
   end

   # Most other control codes show up literally
   def test_inspect_other_control_characters
      assert_equal('"x\\001y\\001"', "x\001y\001".inspect)
      assert_equal('"x\\002y\\002"', "x\002y\002".inspect)
      assert_equal('"x\\003y\\003"', "x\003y\003".inspect)
   end

   def test_inspect_tainted_string
      assert_equal(false, "abc".inspect.tainted?)
      assert_equal(true, "abc".taint.inspect.tainted?)
   end

   def test_inspect_edge_cases
      assert_equal('""', "".inspect)
      assert_equal('" "', " ".inspect)
   end

   def teardown
      @string = nil
      $string = nil
   end
end
