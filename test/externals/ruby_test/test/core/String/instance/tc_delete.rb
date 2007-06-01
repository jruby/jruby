#########################################################################
# tc_delete.rb
#
# Test case for the String#delete and String#delete! instance methods.
#########################################################################
require "test/unit"

class TC_String_Delete_Instance < Test::Unit::TestCase
   def setup
      @str = "<html><b>Hello</b></html>\t\r\n"
   end

   def test_delete_basic
      assert_respond_to(@str, :delete)
      assert_respond_to(@str, :delete!)
      assert_nothing_raised{ @str.delete("h") }
      assert_nothing_raised{ @str.delete!("h") }
   end

   def test_delete
      assert_equal("<htm><b>Heo</b></htm>\t\r\n", @str.delete("l"))
      assert_equal("<ht><b>Heo</b></ht>\t\r\n", @str.delete("lm"))
      assert_equal("<html><b>Hello</b></html>\t\r\n", @str.delete("l", "^l"))
      assert_equal("<><>H</></>\t\r\n", @str.delete("a-z"))
      assert_equal("<html><b>Hello</b></html>\t\r\n", @str.delete("z-a"))
      assert_equal("<html><b>Hello</b></html>", @str.delete("\t\r\n"))
   end

   def test_delete_bang
      assert_equal("<htm><b>Heo</b></htm>\t\r\n", @str.delete!("l"))
      assert_equal("<htm><b>Heo</b></htm>", @str.delete!("\t\r\n"))
      assert_equal(nil, @str.delete!("Z"))
      assert_equal("htmbHeo/b/htm", @str.delete!("<>"))
   end

   def test_delete_expected_errors
      assert_raises(ArgumentError){ @str.delete }
      assert_raises(TypeError){ @str.delete(1) }
   end

   def teardown
      @str = nil
   end
end
