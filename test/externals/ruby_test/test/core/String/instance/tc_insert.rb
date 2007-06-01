######################################################################
# tc_insert.rb
#
# Test case for the String#insert instance method.
######################################################################
require "test/unit"

class TC_String_Insert_Instance < Test::Unit::TestCase
   def setup
      @string = " p1031'/> <b><c n=3D'field'/><c n=3D'fl "
   end

   def test_insert_basic
      assert_respond_to(@string, :insert)
      assert_nothing_raised{ @string.insert(0, 'a') }
      assert_nothing_raised{ @string.insert(-1, 'a') }
   end

   def test_insert
      assert_equal("Xabcd", "abcd".insert(0, 'X'))
      assert_equal("abcXd", "abcd".insert(3, 'X'))
      assert_equal("abcdX", "abcd".insert(4, 'X'))
      assert_equal("abXcd", "abcd".insert(-3, 'X'))
      assert_equal("abcdX", "abcd".insert(-1, 'X'))
   end

   def test_insert_edge_cases
      assert_equal('', ''.insert(0, ''))
      assert_equal(' ', ''.insert(0, ' '))
      assert_equal('', ''.insert(-1, ''))
   end

   def test_insert_destructive
      assert_nothing_raised{ @string.insert(11, '!') }
      assert_equal(" p1031'/> <!b><c n=3D'field'/><c n=3D'fl ", @string)
   end

   def test_insert_expected_errors
      assert_raises(ArgumentError){ @string.insert }
      assert_raises(ArgumentError){ @string.insert(0) }
      assert_raises(TypeError){ @string.insert(0, 0) }
      assert_raises(IndexError){ @string.insert(99, 'a') }
      assert_raises(IndexError){ @string.insert(-99, 'a') }
   end

   def teardown
      @string = nil
   end
end
