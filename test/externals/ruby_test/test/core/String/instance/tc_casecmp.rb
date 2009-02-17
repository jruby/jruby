############################################################
# tc_casecmp.rb
#
# Test case for the String#casecmp instance method.
############################################################
require 'test/unit'

class TC_String_Casecmp_InstanceMethod < Test::Unit::TestCase
   def setup
      @str1 = "<html><b>hello</b></html>"
      @str2 = "<HTML><B>HELLO</B></HTML>"
      @str3 = "<html>hello</html>"
   end

   def test_casecmp_basic
      assert_respond_to(@str1, :casecmp)
      assert_nothing_raised{ @str1.casecmp(@str2) }
   end

   def test_casecmp
      assert_equal(0, @str1.casecmp(@str1))
      assert_equal(0, @str1.casecmp(@str2))
      assert_equal(-1, @str1.casecmp(@str3))
      assert_equal(1, @str3.casecmp(@str1))
   end

   def test_casecmp_edge_cases
      assert_equal(0, ''.casecmp(''))
      assert_equal(-1, ''.casecmp(' '))
      assert_equal(1, ' '.casecmp(''))
      assert_equal(0, '123'.casecmp('123'))
      assert_equal(0, '!@#$%^&*()'.casecmp('!@#$%^&*()'))
   end

   def test_casecmp_expected_errors
      assert_raises(TypeError){ @str1.casecmp(1) }
      assert_raises(ArgumentError){ @str1.casecmp(@str2, @str3) }
   end

   def teardown
      @str1 = nil
      @str2 = nil
      @str3 = nil
   end
end
