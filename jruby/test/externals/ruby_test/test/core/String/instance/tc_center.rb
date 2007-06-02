############################################################
# tc_center.rb
#
# Test case for the String#center instance method.
############################################################
require "test/unit"

class TC_String_Center_Instance < Test::Unit::TestCase
   def setup
      @str = "test"
   end

   def test_center_basic
      assert_respond_to(@str, :center)
      assert_nothing_raised{ @str.center(0) }
   end

   def test_center
      assert_equal("test", @str.center(0))
      assert_equal("test", @str.center(1))
      assert_equal(" test ", @str.center(6))
   end

   def test_center_with_padding
      assert_equal(" test ", @str.center(6, " "))
      assert_equal("!test!", @str.center(6, "!"))
      assert_equal("\ntest\n", @str.center(6, "\n"))
   end

   def test_center_expected_errors
      assert_raises(ArgumentError){ @str.center }
      assert_raises(TypeError){ @str.center("foo") }
      assert_raises(TypeError){ @str.center(6, 1) }
      assert_raises(ArgumentError){ @str.center(6, "") }
   end

   def teardown
      @str = nil
   end
end
