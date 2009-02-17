######################################################################
# tc_chop.rb
#
# Test suite for the String#chop instance method. The String#chop!
# tests can be found in the tc_chop_bang.rb file.
######################################################################
require 'test/unit'

class TC_String_Chop_InstanceMethod < Test::Unit::TestCase
   def setup
      @string_basic  = "hello"
      @string_empty  = ""
      @string_extra  = "hello\n\n\n"
      @string_single = "x"
   end

   def test_chop_basic
      assert_respond_to(@string_basic, :chop)
      assert_nothing_raised{ @string_basic.chop }
      assert_kind_of(String, @string_basic.chop)
   end

   def test_chop
      assert_equal("hell", @string_basic.chop)
      assert_equal("", @string_empty.chop)
      assert_equal("", @string_single.chop)
      assert_equal("hello\n\n", @string_extra.chop)

      # Check to make sure original is unmodified
      assert_equal("hello", @string_basic)
      assert_equal("", @string_empty)
      assert_equal("x", @string_single)
      assert_equal("hello\n\n\n", @string_extra)
   end

   def test_chop_form_feed_and_newline
      assert_equal("hello", "hello\r\n".chop)
      assert_equal("hello\n", "hello\n\r".chop)
   end

   def test_expected_errors
      assert_raises(ArgumentError){ @string_basic.chop('suey') }
   end

   def teardown
      @string_basic  = nil
      @string_empty  = nil
      @string_extra  = nil
      @string_single = nil
   end
end
