######################################################################
# tc_lstrip.rb
#
# Test case for the String#lstrip and String#lstrip! instance methods.
######################################################################
require 'test/unit'

class TC_String_Lstrip_Instance < Test::Unit::TestCase
   def setup
      @string1 = "   hello"
      @string2 = "\0\0\0hello"
      @string3 = "X hello \0 \0"
      @string4 = "hello"
   end

   def test_lstrip_basic
      assert_respond_to(@string1, :lstrip)
      assert_nothing_raised{ @string1.lstrip }
      assert_nothing_raised{ @string2.lstrip }
      assert_nothing_raised{ @string3.lstrip }
      assert_nothing_raised{ @string4.lstrip }
      assert_kind_of(String, @string1.lstrip)
   end

   def test_lstrip_bang_basic
      assert_respond_to(@string1, :lstrip!)
      assert_nothing_raised{ @string1.lstrip! }
      assert_nothing_raised{ @string2.lstrip! }
      assert_nothing_raised{ @string3.lstrip! }
      assert_nothing_raised{ @string4.lstrip! }
      assert_nil(@string1.lstrip!)
   end

   def test_lstrip_edge_cases
      assert_nothing_raised{ "".lstrip }
      assert_equal("\0\0\0hello", @string2.lstrip)
      assert_equal("X hello \0 \0", @string3.lstrip)
      assert_equal("hello", @string4.lstrip)
   end

   def test_lstrip_non_destructive
      assert_nothing_raised{ @string1.lstrip }
      assert_equal("   hello", @string1)
   end

   def test_lstrip_bang_destructive
      assert_nothing_raised{ @string1.lstrip! }
      assert_equal("hello", @string1)
   end

   def test_lstrip_expected_errors
      assert_raises(ArgumentError){ @string1.lstrip(0) }
   end

   def test_lstrip_bang_expected_errors
      assert_raises(ArgumentError){ @string1.lstrip!(0) }
   end

   def teardown
      @string1 = nil
      @string2 = nil
      @string3 = nil
      @string4 = nil
   end
end
