########################################################################
# tc_dump.rb
#
# Test case for the String#dump instance method. This test suite
# was added because string.c has a custom implementation.
#
# TODO: Figure out if there's any actual difference between String#dump
# and String#inspect.
########################################################################
require 'test/unit'

class TC_String_Dump_InstanceMethod < Test::Unit::TestCase
   def setup
      @string = 'hello'
      $string = 'world'
   end

   def test_dump_basic
      assert_respond_to(@string, :dump)
      assert_nothing_raised{ @string.dump }
      assert_kind_of(String, @string.dump)
   end

   def test_dump
      assert_equal('"abc"', "abc".dump)
      assert_equal('"123"', "123".dump)
   end

   def test_dump_literal_quotes
      assert_equal('"\'x\'"', "'x'".dump)
      assert_equal('"\"x\""', "\"x\"".dump)
   end

   def test_dump_backslash
      assert_equal('"\\\\foo\\\\bar"', '\foo\bar'.dump)
      assert_equal('"C:\\\\foo\\\\bar"', "C:\\foo\\bar".dump)
   end

   # See the IS_EVSTR macro in string.c
   def test_dump_special_characters
      string = 'hello'
      assert_equal('"$string"', "$string".dump)
      assert_equal('"@string"', "@string".dump)
      assert_equal('"#string"', "#string".dump)
      assert_equal('"hello"', "#{string}".dump)
      assert_equal('"world"', "#$string".dump)
      assert_equal('"hello"', "#@string".dump)
   end

   def test_dump_with_newline
      assert_equal('"x\\ny\\n"', "x\ny\n".dump)
      assert_equal('"x\\ry\\r"', "x\ry\r".dump)
   end

   def test_dump_with_tab
      assert_equal('"x\\ty\\t"', "x\ty\t".dump)
   end

   def test_dump_with_form_feed
      assert_equal('"x\\fy\\f"', "x\fy\f".dump)
   end

   # These are specially handled in string.c
   def test_dump_special_control_characters
      assert_equal('"x\\ay\\a"', "x\007y\007".dump) # Beep
      assert_equal('"x\\by\\b"', "x\010y\010".dump) # Backspace
      assert_equal('"x\\vy\\v"', "x\013y\013".dump) # Vertical tab
      assert_equal('"x\\ey\\e"', "x\033y\033".dump) # Escape
   end

   # Most other control codes show up literally
   def test_dump_other_control_characters
      assert_equal('"x\\001y\\001"', "x\001y\001".dump)
      assert_equal('"x\\002y\\002"', "x\002y\002".dump)
      assert_equal('"x\\003y\\003"', "x\003y\003".dump)
   end

   def test_dump_tainted_string
      assert_equal(false, "abc".dump.tainted?)
      assert_equal(true, "abc".taint.dump.tainted?)
   end

   def test_dump_edge_cases
      assert_equal('""', "".dump)
      assert_equal('" "', " ".dump)
   end

   def teardown
      @string = nil
      $string = nil
   end
end
