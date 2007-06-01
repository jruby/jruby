###########################################################
# tc_equality.rb
#
# Test case for the String#== instance method.
###########################################################
require "test/unit"

class TC_String_Equality_Instance < Test::Unit::TestCase
   def setup
      @str1 = %q{ p1031'/> <b><c n=3D'field'/><c n=3D'fl }
      @str2 = %q{ p1031'/> <b><c n=3D'field'/><c n=3D'fl }
      @str3 = %q{ p1031'/> <B><c N=3D'field'/><c n=3D'Fl }
   end

   def test_string_basic
      assert_respond_to(@str1, :==)
      assert_nothing_raised{ @str1 == @str2 }
   end

   def test_string_equality
      assert_equal(true, @str1 == @str1)
      assert_equal(true, @str1 == @str2)
      assert_equal(true, "" == "")

      assert_equal(false, @str1 == @str3)
      assert_equal(false, @str1 == nil)
      assert_equal(false, @str1 == true)
      assert_equal(false, @str1 == false)
      assert_equal(false, @str1 == 0)
      assert_equal(false, "foo" == :foo)
   end

   def teardown
      @str1 = nil
      @str2 = nil
      @str3 = nil
   end
end
