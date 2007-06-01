############################################################################
# tc_downcase.rb
#
# Test case for the String#downcase and String#downcase! instance methods.
############################################################################
require "test/unit"

class TC_String_Downcase_Instance < Test::Unit::TestCase
   def setup
      @str = "<HTML><B>HELLO</B></HTML>"
   end

   def test_downcase_basic
      assert_respond_to(@str, :downcase)
      assert_respond_to(@str, :downcase!)
      assert_nothing_raised{ @str.downcase }
      assert_nothing_raised{ @str.downcase! }
   end

   def test_downcase
      assert_equal("<html><b>hello</b></html>", @str.downcase)
      assert_equal("<HTML><B>HELLO</B></HTML>", @str)
      assert_equal("hello", "hello".downcase)
   end

   def test_downcase_bang
      assert_equal("<html><b>hello</b></html>", @str.downcase!)
      assert_equal("<html><b>hello</b></html>", @str)
      assert_equal(nil, "hello".downcase!)
   end

   def test_downcase_expected_errors
      assert_raises(ArgumentError){ @str.downcase("test") }
      assert_raises(ArgumentError){ @str.downcase!("test") }
   end

   def teardown
      @str = nil
   end
end
