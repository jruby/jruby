######################################################################
# tc_rjust.rb
#
# Test case for the String#rjust instance method.
######################################################################
require "test/unit"

class TC_String_Rjust_Instance < Test::Unit::TestCase
   def setup
      @string = 'hello'
   end

   def test_rjust_basic
      assert_respond_to(@string, :rjust)
      assert_nothing_raised{ @string.rjust(0) }
      assert_nothing_raised{ @string.rjust(0, 'x') }
      assert_kind_of(String, @string.rjust(0))
   end

   def test_rjust
      assert_equal('hello', @string.rjust(0))
      assert_equal('   hello', @string.rjust(8))
      assert_equal('XXXhello', @string.rjust(8, 'X'))
      assert_equal("   hello", "   hello".rjust(8))
   end

   def test_rjust_non_destructive
      string2 = @string.rjust(8, 'X')
      assert_not_equal(string2.object_id, @string.object_id)
      assert_equal('hello', @string)
   end

   def test_rjust_edge_cases
      assert_equal('hello', @string.rjust(-1))
      assert_equal('hello', @string.rjust(-100))
      assert_equal("\0\0\0\0\0hello", @string.rjust(10, "\0"))
      assert_equal("xxhello\0\0\0", "hello\0\0\0".rjust(10, 'x'))
   end

   def test_rjust_expected_errors
      assert_raises(ArgumentError){ @string.rjust }
      assert_raises(ArgumentError){ @string.rjust(0, '') }
      assert_raises(TypeError){ @string.rjust(0, nil) }
      assert_raises(TypeError){ @string.rjust(10, 7) }
      assert_raises(TypeError){ @string.rjust('a') }
   end

   def teardown
      @string = nil
   end
end
