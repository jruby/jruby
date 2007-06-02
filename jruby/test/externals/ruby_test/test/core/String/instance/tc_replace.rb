######################################################################
# tc_replace.rb
#
# Test case for the String#replace instance method.
######################################################################
require "test/unit"

class TC_String_Replace_Instance < Test::Unit::TestCase
   def setup
      @string1 = "<html><b>hello</b></html>"
      @string2 = @string1
   end

   def test_replace_basic
      assert_respond_to(@string1, :replace)
      assert_nothing_raised{ @string1.replace("") }
   end

   def test_replace
      assert_equal("x,y,z", @string1.replace("x,y,z"))
      assert_equal("x,y,z", @string1)
      assert_equal(@string2, @string1)
      assert_equal(@string2.object_id, @string1.object_id)
   end

   def test_replace_expected_errors
      assert_raises(ArgumentError){ @string1.replace("x","y") }
      assert_raises(TypeError){ @string1.replace(1) }
   end

   def teardown
      @string1 = nil
      @string2 = nil
   end
end
