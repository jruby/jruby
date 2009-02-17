#########################################################################
# tc_delete_bang_bang.rb
#
# Test case for the String#delete! instance method. The tests for the
# String#delete method are in the tc_delete.rb file.
#########################################################################
require 'test/unit'

class TC_String_DeleteBang_InstanceMethod < Test::Unit::TestCase
   def setup
      @str = "<html><b>Hello</b></html>\t\r\n"
   end

   def test_delete_bang_basic
      assert_respond_to(@str, :delete!)
      assert_nothing_raised{ @str.delete!('h') }
      assert_kind_of(String, @str.delete!('e'))
   end

   def test_delete
      assert_equal("<htm><b>Heo</b></htm>\t\r\n", @str.delete!('l'))
      assert_equal("<ht><b>Heo</b></ht>\t\r\n", @str.delete!('lm'))
      assert_equal("<ht><b>Heo</b></ht>", @str.delete!("\t\r\n"))
   end

   def test_delete_bang_with_negation
      assert_equal(nil, @str.delete!('lll', '^l'))
      assert_equal(nil, 'hello'.delete!('l', '^l'))
      assert_equal('heo', 'hello'.delete!('l', '^o'))
   end

   def test_delete_bang_with_range
      assert_equal("<><>H</></>\t\r\n", @str.delete!('a-z'))
      assert_equal('', 'hello'.delete!('a-z'))
      assert_equal('e', 'hello'.delete!('f-z'))
   end

   def test_delete_bang_with_range_and_negation
      @str = 'hello'
      assert_equal('o', @str.delete!('a-z', '^o'))
   end

   def test_delete_bang_original_string_modified
      @str = 'hello'
      assert_nothing_raised{ @str.delete!('l') }
      assert_equal('heo', @str)
   end

   def test_delete_bang_edge_cases
      assert_equal(nil,  ''.delete!(''))
      assert_equal(nil,  ''.delete!('^'))
      assert_equal(nil,  ''.delete!('x'))
      assert_equal('',  ' '.delete!(' '))
      assert_equal(nil,  ' '.delete!(' ', '^ '))
      assert_equal(nil, 'x'.delete!(''))
      assert_equal(nil, 'x'.delete!('', ''))
      assert_equal(nil, 'hello'.delete!('z-a'))
   end

   def test_delete_bang_expected_errors
      assert_raises(ArgumentError){ @str.delete! }
      assert_raises(TypeError){ @str.delete!(1) }
      assert_raises(TypeError){ @str.delete!(nil) }
      assert_raises(TypeError){ @str.delete!(true) }
      assert_raises(TypeError){ @str.delete!('a'..'z') }
   end

   def teardown
      @str = nil
   end
end
