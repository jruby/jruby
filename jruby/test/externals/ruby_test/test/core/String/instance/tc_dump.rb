#################################################################
# tc_dump.rb
#
# Test case for the String#dump method.
#
# TODO: needs better tests
#################################################################
require "test/unit"

class TC_String_Dump_Instance < Test::Unit::TestCase
   def setup   
      @string = "replace me with something clever"
   end

   def test_dump_basic
      assert_respond_to(@string, :dump)
      assert_nothing_raised{ @string.dump }
      assert_kind_of(String, @string.dump)
   end

   def test_dump
      assert_equal("\"#{@string}\"", @string.dump)
   end

   def test_string_expected_errors
      assert_raises(ArgumentError){ @string.dump("test") }
   end

   def teardown
      @string = nil
   end
end
