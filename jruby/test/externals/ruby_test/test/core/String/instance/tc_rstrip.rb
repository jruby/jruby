######################################################################
# tc_rstrip.rb
#
# Test case for the String#rstrip and String#rstrip! instance methods.
######################################################################
require 'test/unit'

class TC_String_Rstrip_Instance < Test::Unit::TestCase
   def setup
      @string1 = "hello   "
      @string2 = "hello\0\0\0"
      @string3 = "\0 \0 hello X"
      @string4 = "hello"
   end

   def test_rstrip_basic
      assert_respond_to(@string1, :rstrip)
      assert_nothing_raised{ @string1.rstrip }
      assert_nothing_raised{ @string2.rstrip }
      assert_nothing_raised{ @string3.rstrip }
      assert_nothing_raised{ @string4.rstrip }
      assert_kind_of(String, @string1.rstrip)
   end

   def test_rstrip_bang_basic
      assert_respond_to(@string1, :rstrip!)
      assert_nothing_raised{ @string1.rstrip! }
      assert_nothing_raised{ @string2.rstrip! }
      assert_nothing_raised{ @string3.rstrip! }
      assert_nothing_raised{ @string4.rstrip! }
      assert_nil(@string1.rstrip!)
   end

   def test_rstrip_edge_cases
      assert_nothing_raised{ "".rstrip }
      assert_equal("hello", @string2.rstrip)
      assert_equal("\0 \0 hello X", @string3.rstrip)
      assert_equal("hello", @string4.rstrip)
   end

   def test_rstrip_non_destructive
      assert_nothing_raised{ @string1.rstrip }
      assert_equal("hello   ", @string1)
   end

   def test_rstrip_bang_destructive
      assert_nothing_raised{ @string1.rstrip! }
      assert_equal("hello", @string1)
   end

   def test_rstrip_expected_errors
      assert_raises(ArgumentError){ @string1.rstrip(0) }
   end

   def test_rstrip_bang_expected_errors
      assert_raises(ArgumentError){ @string1.rstrip!(0) }
   end

   def teardown
      @string1 = nil
      @string2 = nil
      @string3 = nil
      @string4 = nil
   end
end
