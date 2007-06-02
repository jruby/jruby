######################################################################
# tc_format.rb
#
# Test case for the String#% instance method.
#
# TODO: Add more tests
######################################################################
require "test/unit"

class TC_String_Format_Instance < Test::Unit::TestCase
   def setup
      @string1 = "%05d"
      @string2 = "%-5s: %08x"
   end

   def test_format_basic
      assert_respond_to(@string1, :%)
      assert_nothing_raised{ @string1 % 123 }
   end

   def test_format
      assert_equal("00123", @string1 % 123)
      assert_equal("ID   : 000003db", @string2 % ["ID", 987])
   end

   def test_format_expected_errors
      assert_raises(TypeError){ @string1 % {"ID" => 987} }
   end

   def teardown
      @string1 = nil
      @string2 = nil
   end
end
