############################################################
# tc_crypt.rb
#
# Test case for the String#crypt instance method.
############################################################
require 'test/unit'

class TC_String_Crypt_InstanceMethod < Test::Unit::TestCase
   def setup
      @str = "<html><b>Hello</b></html>\r\n\t"
   end

   def test_crypt
      assert_respond_to(@str, :crypt)
      assert_nothing_raised{ @str.crypt('sh') }
      assert_nothing_raised{ @str.crypt('pwqpha;shl;ja823549874') }
      assert_kind_of(String, @str.crypt('sh'))
   end

   # If the original string or the salt is tainted, the result is tainted
   def test_crypt_tainted_string
      assert_equal(false, 'test'.crypt('sh').tainted?)
      assert_equal(true, 'test'.taint.crypt('sh').tainted?)
      assert_equal(true, 'test'.crypt('sh'.taint).tainted?)
   end

   def test_crypt_expected_errors
      assert_raise(TypeError){ @str.crypt(128) }
      assert_raise(ArgumentError){ @str.crypt('foo', 'bar') }
      assert_raise(ArgumentError){ @str.crypt('s') } # String too short
   end

   def teardown
      @str = nil
   end
end
