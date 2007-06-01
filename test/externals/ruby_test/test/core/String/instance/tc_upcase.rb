############################################################################
# tc_upcase.rb
#
# Test case for the String#upcase and String#upcase! instance methods.
############################################################################
require "test/unit"

class TC_String_Upcase_Instance < Test::Unit::TestCase
   def setup
      @str = "<html><b>hello</b></html>"
   end

   def test_upcase_basic
      assert_respond_to(@str, :upcase)
      assert_respond_to(@str, :upcase!)
      assert_nothing_raised{ @str.upcase }
      assert_nothing_raised{ @str.upcase! }
   end

   def test_upcase
      assert_equal("<HTML><B>HELLO</B></HTML>", @str.upcase)
      assert_equal("<html><b>hello</b></html>", @str)

      assert_equal("HELLO", "HELLO".upcase)
      assert_equal('', ''.upcase)
   end

   def test_upcase_bang
      assert_equal("<HTML><B>HELLO</B></HTML>", @str.upcase!)
      assert_equal("<HTML><B>HELLO</B></HTML>", @str)

      assert_equal(nil, "HELLO".upcase!)
      assert_equal(nil, ''.upcase!)
   end

   def test_upcase_expected_errors
      assert_raises(ArgumentError){ @str.upcase("test") }
      assert_raises(ArgumentError){ @str.upcase!("test") }
   end

   def teardown
      @str = nil
   end
end
