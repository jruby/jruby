######################################################################
# tc_unpack.rb
#
# Test case for the String#unpack instance method.
######################################################################
require 'test/unit'

class TC_String_Unpack_Instance < Test::Unit::TestCase
   def setup
      @string1 = "hello"
      @string2 = "hello    "
      @string3 = "hello    \0\0\0   "
   end

   def test_unpack_basic
      assert_respond_to(@string1, :unpack)
   end

   def test_unpack_A
      assert_equal(['h'], @string1.unpack('A'))
      assert_equal(['he'], @string1.unpack('A2'))

      assert_equal(['hello'], @string1.unpack("A*"))
      assert_equal(['hello'], @string2.unpack("A*"))
      assert_equal(['hello'], @string3.unpack("A*"))
   end

   def test_unpack_a
      assert_equal(['h'], @string1.unpack('a'))
      assert_equal(['he'], @string1.unpack('a2'))

      assert_equal(['hello'], @string1.unpack("a*"))
      assert_equal(['hello    '], @string2.unpack("a*"))
      assert_equal(["hello    \0\0\0   "], @string3.unpack("a*"))
   end

   def teardown
      @string = nil
   end
end
