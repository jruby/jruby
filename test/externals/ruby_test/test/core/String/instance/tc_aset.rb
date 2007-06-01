###########################################################
# tc_aset.rb
#
# Test suite for the String#[]= instance method.
###########################################################
require "test/unit"

class TC_String_Aset_Instance < Test::Unit::TestCase
   def setup
      @string1 = "qwerty"
      @string2 = "<strong>hello</strong>"
   end

   def test_aset_basic
      assert_respond_to(@string1, :[]=)
   end

   # str[int]
   def test_aset_index
      assert_equal('x', @string1[0] = 'x')
      assert_equal('xwerty', @string1)

      assert_equal('z', @string1[-1] = 'z')
      assert_equal('xwertz', @string1)
   end

   # str[int, int]
   def test_aset_substring
      assert_equal('fli', @string1[0,3] = 'fli')
      assert_equal('flirty', @string1)

      assert_equal('z', @string1[-1,1] = 'z')
      assert_equal('flirtz', @string1)

      assert_equal('di', @string1[0,3] = 'di')
      assert_equal('dirtz', @string1)
   end

   # str[range]
   def test_aset_range
      assert_equal('fli', @string1[0..2] = 'fli')
      assert_equal('flirty', @string1)

      assert_equal('z', @string1[-1..-1] = 'z')
      assert_equal('flirtz', @string1)

      assert_equal('di', @string1[0..2] = 'di')
      assert_equal('dirtz', @string1)
   end

   # str[regexp]
   def test_aset_regexp
      assert_equal('bold', @string2[/strong/] = 'bold')
      assert_equal('<bold>hello</strong>', @string2)
   end

   # str[regexp, int]
   def test_aset_regexp_matchdata
   end

   # str[str]
   def test_aset_string
   end

   def test_aset_edge_cases
   end

   def test_aset_expected_errors
      assert_raises(IndexError){ @string1[99] = 'z' }
      assert_raises(IndexError){ @string1[-99] = 'z' }

      assert_raises(IndexError){ @string1[2,-1] = 'qw' }

      assert_raises(IndexError){ @string1[/foobar/] = 'foo' }
   end

   def teardown
      @string1 = nil
      @string2 = nil
   end
end
