######################################################################
# tc_hex.rb
#
# Test case for the String#hex instance method.
######################################################################
require "test/unit"

class TC_String_Hex_Instance < Test::Unit::TestCase
   def setup
      @string1 = "0x0F"
      @string2 = "-1234"
      @string3 = "hello"
      @string4 = "heckx0F"
   end

   def test_hex_basic
      assert_respond_to(@string1, :hex)
      assert_nothing_raised{ @string1.hex }
   end

   def test_hex
      assert_equal(15, @string1.hex)
      assert_equal(-4660, @string2.hex)
      assert_equal(0, @string3.hex)
      assert_equal(0, @string4.hex)

      assert_nothing_raised{ "nil".hex }
      assert_nothing_raised{ "true".hex }
      assert_nothing_raised{ "false".hex }
   end

   def test_hex_expected_errors
      assert_raises(ArgumentError){ @string1.hex(1) }
   end

   def teardown
      @string1 = nil
      @string2 = nil
      @string3 = nil
      @string4 = nil
   end
end
