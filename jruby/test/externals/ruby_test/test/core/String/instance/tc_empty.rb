######################################################################
# tc_empty.rb
#
# Test case for the String#empty? instance method.
######################################################################
require "test/unit"

class TC_String_Empty_Instance < Test::Unit::TestCase
   def setup
      @string1 = ""
      @string2 = ''
      @string3 = " "
   end

   def test_empty_basic
      assert_respond_to(@string1, :empty?)
      assert_nothing_raised{ @string1.empty? }
   end

   def test_empty
      assert_equal(true, @string1.empty?)
      assert_equal(true, @string2.empty?)
      assert_equal(false, @string3.empty?)
   end

   def test_empty_expected_errors
      assert_raises(ArgumentError){ @string1.empty?(1) }
   end

   def teardown
      @string1 = nil
      @string2 = nil
      @string3 = nil
   end
end
