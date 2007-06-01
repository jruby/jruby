############################################################
# tc_crypt.rb
#
# Test case for the String#crypt instance method.
############################################################
require "test/unit"

class TC_String_Crypt_Instance < Test::Unit::TestCase
   def setup
      @str = "<html><b>Hello</b></html>\r\n\t"
   end

   def test_crypt
      assert_respond_to(@str, :crypt)
      assert_nothing_raised{ @str.crypt("sh") }
      assert_nothing_raised{ @str.crypt("pwqpha;shl;ja823549874") }
   end

   def test_crypt_expected_errors
      assert_raises(TypeError){ @str.crypt(128) }
      assert_raises(ArgumentError){ @str.crypt("foo","bar") }
   end

   def teardown
      @str = nil
   end
end
