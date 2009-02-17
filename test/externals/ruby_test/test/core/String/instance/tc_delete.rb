#########################################################################
# tc_delete.rb
#
# Test case for the String#delete instance method. The tests for the
# String#delete! method are in the tc_delete_bang.rb file.
#########################################################################
require 'test/unit'

class TC_String_Delete_InstanceMethod < Test::Unit::TestCase
   def setup
      @str = "<html><b>Hello</b></html>\t\r\n"
   end

   def test_delete_basic
      assert_respond_to(@str, :delete)
      assert_nothing_raised{ @str.delete('h') }
      assert_kind_of(String, @str.delete('h'))
   end

   def test_delete
      assert_equal("<htm><b>Heo</b></htm>\t\r\n", @str.delete('l'))
      assert_equal("<ht><b>Heo</b></ht>\t\r\n", @str.delete('lm'))
      assert_equal("<html><b>Hello</b></html>", @str.delete("\t\r\n"))
   end

   def test_delete_with_negation
      assert_equal("<html><b>Hello</b></html>\t\r\n", @str.delete('lll', '^l'))
      assert_equal('hello', 'hello'.delete('l', '^l'))
      assert_equal('heo', 'hello'.delete('l', '^o'))
   end

   def test_delete_with_range
      assert_equal("<><>H</></>\t\r\n", @str.delete('a-z'))
      assert_equal('', 'hello'.delete('a-z'))
      assert_equal('e', 'hello'.delete('f-z'))
   end

   def test_delete_with_range_and_negation
      @str = 'hello'
      assert_equal('o', @str.delete('a-z', '^o'))
   end

   def test_delete_original_string_unmodified
      @str = 'hello'
      assert_nothing_raised{ @str.delete('l') }
      assert_equal('hello', @str)
   end

   def test_delete_edge_cases
      assert_equal('',  ''.delete(''))
      assert_equal('',  ''.delete('^'))
      assert_equal('',  ''.delete('x'))
      assert_equal('',  ' '.delete(' '))
      assert_equal(' ',  ' '.delete(' ', '^ '))
      assert_equal('x', 'x'.delete(''))
      assert_equal('x', 'x'.delete('', ''))
      assert_equal('hello', 'hello'.delete('z-a'))
   end

   def test_delete_expected_errors
      assert_raises(ArgumentError){ @str.delete }
      assert_raises(TypeError){ @str.delete(1) }
      assert_raises(TypeError){ @str.delete(nil) }
      assert_raises(TypeError){ @str.delete(true) }
      assert_raises(TypeError){ @str.delete('a'..'z') }
   end

   def teardown
      @str = nil
   end
end
