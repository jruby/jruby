###############################################################################
# tc_each.rb
#
# Test case for the String#each instance method, as well as the
# String#each_line alias.
###############################################################################
require 'test/unit'

class TC_String_Each_InstanceMethod < Test::Unit::TestCase
   def setup
      @string = "hello\nworld\txxx\nyyy"
      @array  = []
   end

   def test_each_basic
      assert_respond_to(@string, :each)
      assert_nothing_raised{ @string.each{ } }
      assert_kind_of(String, @string.each{ })
   end

   def test_each_line_alias_basic
      assert_respond_to(@string, :each_line)
      assert_nothing_raised{ @string.each_line{ } }
      assert_kind_of(String, @string.each_line{ })
   end

   def test_each
      assert_nothing_raised{ @string.each{ |s| @array << s } }
      assert_equal(["hello\n", "world\txxx\n", "yyy"], @array)
   end

   def test_each_line_alias
      assert_nothing_raised{ @string.each_line{ |s| @array << s } }
      assert_equal(["hello\n", "world\txxx\n", "yyy"], @array)
   end

   def test_each_with_separator
      assert_nothing_raised{ @string.each("\t"){ |s| @array << s } }
      assert_equal(["hello\nworld\t", "xxx\nyyy"], @array)
   end

   def test_each_line_alias_with_separator
      assert_nothing_raised{ @string.each_line("\t"){ |s| @array << s } }
      assert_equal(["hello\nworld\t", "xxx\nyyy"], @array)
   end

   def test_each_with_empty_separator
      assert_nothing_raised{ "hello\n\n\nworld".each(''){ |s| @array << s } }
      assert_equal(["hello\n\n\n", "world"], @array)
   end

   def test_each_line_alias_with_empty_separator
      assert_nothing_raised{ "hello\n\n\nworld".each_line(''){ |s| @array << s } }
      assert_equal(["hello\n\n\n", "world"], @array)
   end

   def test_each_edge_cases
      assert_nothing_raised{ ''.each{ |s| @array << s } }
      assert_equal([], @array)
   end

   def test_each_expected_errors
      assert_raise(ArgumentError){ @string.each('x', 'y'){ } }
      assert_raise(TypeError){ @string.each(1){ } }
   end

   def test_each_line_alias_expected_errors
      assert_raise(ArgumentError){ @string.each_line('x', 'y'){ } }
      assert_raise(TypeError){ @string.each_line(1){ } }
   end

   def teardown
      @string = nil
      @array  = nil
   end
end
