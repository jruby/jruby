######################################################################
# tc_chop.rb
#
# Test suite for the String#chop and String#chop! instance methods.
######################################################################
require "test/unit"

class TC_String_Chop_Instance < Test::Unit::TestCase
   def setup
      @string_basic  = "hello"
      @string_empty  = ""
      @string_extra  = "hello\n\n\n"
      @string_single = "x"
   end

   def test_chop_basic
      assert_respond_to(@string_basic, :chop)
      assert_nothing_raised{ @string_basic.chop }
   end

   def test_chop_behavior
      assert_equal("hell", @string_basic.chop)
      assert_equal("", @string_empty.chop)
      assert_equal("", @string_single.chop.chop)
      assert_equal("hello\n\n", @string_extra.chop)

      assert_equal("hello", @string_basic)
      assert_equal("", @string_empty)
      assert_equal("x", @string_single)
      assert_equal("hello\n\n\n", @string_extra)
   end

   def test_chop_bang_basic
      assert_respond_to(@string_basic, :chop!)
      assert_nothing_raised{ @string_basic.chop! }
   end

   def test_chop_bang_behavior
      assert_equal("hell", @string_basic.chop!)
      assert_equal(nil, @string_empty.chop!)
      assert_equal(nil, @string_single.chop!.chop!)
      assert_equal("hello\n\n", @string_extra.chop!)

      assert_equal("hell", @string_basic)
      assert_equal("", @string_empty)
      assert_equal("", @string_single)
      assert_equal("hello\n\n", @string_extra)
   end

   def test_expected_errors
      assert_raises(ArgumentError){ @string_basic.chop("suey") }
   end

   def teardown
      @string_basic  = nil
      @string_empty  = nil
      @string_extra  = nil
      @string_single = nil
   end
end
