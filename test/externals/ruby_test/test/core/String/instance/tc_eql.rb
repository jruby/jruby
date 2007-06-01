######################################################################
# tc_eql.rb
#
# Test case for the String#eql instance method.
######################################################################
require "test/unit"

class TC_String_Eql_Instance < Test::Unit::TestCase
   def setup
      @str1 = "<html><b>Hello</b></html>"
      @str2 = "<html><b>Hello</b></html>"
      @str3 = "<html><B>Hello</B></html>"
   end

   def test_eql_basic
      assert_respond_to(@str1, :eql?)
      assert_nothing_raised{ @str1.eql?(@str2) }
      assert_nothing_raised{ @str1.eql?(nil) }
      assert_nothing_raised{ @str1.eql?(false) }
      assert_nothing_raised{ @str1.eql?(true) }
      assert_nothing_raised{ @str1.eql?(0) }
   end

   def test_eql
      assert_equal(true, @str1.eql?(@str1))
      assert_equal(true, @str1.eql?(@str2))

      assert_equal(false, @str1.eql?(@str3))
      assert_equal(false, @str1.eql?(nil))
      assert_equal(false, @str1.eql?(false))
      assert_equal(false, @str1.eql?(true))
      assert_equal(false, @str1.eql?(0))
   end

   def test_eql_expected_errors
      assert_raises(ArgumentError){ @str1.eql?(@str2, @str3) }
   end

   def teardown
      @str1 = nil
      @str2 = nil
      @str3 = nil
   end
end
