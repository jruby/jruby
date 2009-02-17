#####################################################################
# tc_split.rb
#
# Test case for the String#split method.
#####################################################################
require 'test/unit'

class TC_String_Split_Instance < Test::Unit::TestCase
   def setup
      @string1 = " now's the time"
      @string2 = "a@1bb@2ccc"
      @string3 = "1, 2.34,56, 7"
      @string4 = "hello"
   end

   def test_split_basic
      assert_respond_to(@string1, :split)
      assert_nothing_raised{ @string1.split }
      assert_nothing_raised{ @string1.split('', 1) }
   end

   def test_split_whitespace
      assert_equal(["now's", "the", "time"], @string1.split)
      assert_equal(["now's", "the", "time"], @string1.split(' '))
      assert_equal(["now's", "the", "time"], @string1.split(nil))
      assert_equal(["", "now's", "the", "time"], @string1.split(/\s+/))
      assert_equal(["", "now's", "the", "time"], @string1.split(/ /))
   end

   def test_split_regular_expression
      assert_equal(['a', 'bb', 'ccc'], @string2.split(/@\d/))
      assert_equal(['a', '1', 'bb', '2', 'ccc'], @string2.split(/@(\d)/))
      assert_equal(['1', '2.34', '56', '7'], @string3.split(/,\s*/))
      assert_equal(['h', 'e', 'l', 'l', 'o'], @string4.split(//))
   end

   def test_split_regular_expression_with_limit
      assert_equal(['a', 'bb', 'ccc'], @string2.split(/@\d/, 0))
      assert_equal([@string2], @string2.split(/@\d/, 1))
      assert_equal(['a', 'bb@2ccc'], @string2.split(/@\d/, 2))
      assert_equal(['h', 'e', 'l', 'l', 'o', ''], @string4.split(//, -1))
   end

   def test_split_with_positive_limit
      assert_equal([" now's the time"], @string1.split(' ', 1))
      assert_equal(["now's", "the time"], @string1.split(' ', 2))
      assert_equal(['h', 'e', 'llo'], @string4.split(//, 3))
   end

   def test_split_with_zero_or_negative_limit
      assert_equal(["now's", "the", "time"], @string1.split(' ', 0))
      assert_equal(["now's", "the", "time"], @string1.split(' ', -1))
      assert_equal(["hello", ""], "hello    ".split(' ', -1))
      assert_equal(['1', '2', '', '3', '', ''], '1,2,,3,,'.split(',', -4))
   end

   def test_split_edge_cases
      assert_equal([], ''.split)
      assert_equal([], ''.split(' '))
      assert_equal([], ''.split(' ', 0))
      assert_equal([], ''.split(' ', -1))
   end

   def test_split_expected_errors
      assert_raises(TypeError){ @string1.split(1) }
      assert_raises(TypeError){ @string1.split(false) }
      assert_raises(ArgumentError){ @string1.split('', 1, 1) }
   end

   def teardown
      @string1 = nil
      @string2 = nil
      @string3 = nil
      @string4 = nil
   end
end
