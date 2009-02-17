######################################################################
# tc_ljust.rb
#
# Test case for the String#ljust instance method.
######################################################################
require "test/unit"

class TC_String_Ljust_InstanceMethod < Test::Unit::TestCase
   def setup
      @string = 'hello'
   end

   def test_ljust_basic
      assert_respond_to(@string, :ljust)
      assert_nothing_raised{ @string.ljust(0) }
      assert_nothing_raised{ @string.ljust(0, 'x') }
      assert_kind_of(String, @string.ljust(0))
   end

   def test_ljust
      assert_equal('hello', @string.ljust(0))
      assert_equal('hello   ', @string.ljust(8))
      assert_equal('helloXXX', @string.ljust(8, 'X'))
      assert_equal("   hello", "   hello".ljust(8))
   end

   def test_ljust_non_destructive
      string2 = @string.ljust(8, 'X')
      assert_not_equal(string2.object_id, @string.object_id)
      assert_equal('hello', @string)
   end

   def test_ljust_edge_cases
      assert_equal('hello', @string.ljust(-1))
      assert_equal('hello', @string.ljust(-100))
      assert_equal("hello\0\0\0\0\0", @string.ljust(10, "\0"))
      assert_equal("hello\0\0\0xx", "hello\0\0\0".ljust(10, 'x'))
   end

   def test_ljust_tainted_string
      assert_equal(false, 'hello'.ljust(8).tainted?)
      assert_equal(true, 'hello'.taint.ljust(8).tainted?)
      # Fails
      #assert_equal(false, 'hello'.ljust(4, 'X'.taint).tainted?) # Not > length
      assert_equal(true, 'hello'.ljust(8, 'X'.taint).tainted?)
   end

   def test_ljust_expected_errors
      assert_raises(ArgumentError){ @string.ljust }
      assert_raises(ArgumentError){ @string.ljust(0, '') }
      assert_raises(TypeError){ @string.ljust(0, nil) }
      assert_raises(TypeError){ @string.ljust(10, 7) }
      assert_raises(TypeError){ @string.ljust('a') }
   end

   def teardown
      @string = nil
   end
end
