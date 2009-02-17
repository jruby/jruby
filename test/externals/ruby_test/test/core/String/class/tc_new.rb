########################################
# tc_new.rb
#
# Test suite for the String#new method.
########################################
require "test/unit"

# Used to verify that objects with .to_str implemented are acceptable
class NewString
   def to_str
      "test"
   end
end

class TC_String_New_Class < Test::Unit::TestCase
   def test_constructor_long
      assert_respond_to(String, :new)
      assert_nothing_raised{ String.new }
      assert_nothing_raised{ String.new("") }
      assert_nothing_raised{ String.new("hello") }
      assert_nothing_raised{ String.new("9") }
   end

   def test_constructor_short
      assert_nothing_raised{ string = "" }
      assert_nothing_raised{ string = "hello" }
      assert_nothing_raised{ string = "9" }
   end

   def test_constructor_allows_to_str
      assert_nothing_raised{ String.new(NewString.new) }
      assert_equal("test", String.new(NewString.new))
   end

   def test_expected_errors
      assert_raise(TypeError){ String.new(nil) }
      assert_raise(TypeError){ String.new(7) }
      assert_raise(ArgumentError, RangeError){ String.new(0.chr * (2**64)) }
   end
end
