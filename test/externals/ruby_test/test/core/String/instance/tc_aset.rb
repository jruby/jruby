###########################################################
# tc_aset.rb
#
# Test suite for the String#[]= instance method.
###########################################################
require "test/unit"

class TC_String_Aset_InstanceMethod < Test::Unit::TestCase
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

   def test_aset_regexp_case_insensitive
      assert_equal('bold', @string2[/STRONG/i] = 'bold')
      assert_equal('<bold>hello</strong>', @string2)
   end

   # str[regexp, int]
   def test_aset_regexp_matchdata
      string = 'hello'
      assert_nothing_raised{ string[/[aeiou](.)\1(.)/, 1] = 'xyz' }
      assert_equal('hexyzlo', string)

      string = 'hello'
      assert_nothing_raised{ string[/[aeiou](.)\1(.)/, 2] = 'xyz' }
      assert_equal('hellxyz', string)
   end

   # str[str]
   def test_aset_string
      assert_nothing_raised{ @string1['q'] = 'f' }
      assert_equal('fwerty', @string1)

      assert_nothing_raised{ @string1['fw'] = 'b' }
      assert_equal('berty', @string1)
 
      assert_nothing_raised{ @string1['b'] = 'qw' }
      assert_equal('qwerty', @string1)
   end

   # If the replacement string is tainted, the original string becomes tainted
   def test_aset_tainted_string
      str = 'x'
      assert_equal(false, @string1.tainted?)
      assert_nothing_raised{ @string1['q'] = str }
      assert_equal(false, @string1.tainted?)
         
      str.taint
      assert_nothing_raised{ @string1['x'] = str }
      assert_equal(true, @string1.tainted?)
   end

   def test_aset_edge_cases
      assert_nothing_raised{ @string1[''] = '' }
      assert_nothing_raised{ @string1[''] = 'hello' }
      assert_equal('helloqwerty', @string1)

      assert_nothing_raised{ @string1[0, 99] = 'x' }
      assert_equal('x', @string1)
   end

   def test_aset_index_expected_errors
      assert_raise(TypeError){ @string1[0] = nil }
      assert_raises(IndexError){ @string1[99] = 'z' }
      assert_raises(IndexError){ @string1[-99] = 'z' }
      assert_raises(IndexError){ @string1[2,-1] = 'qw' }
   end

   def test_aset_index_and_length_expected_errors
      assert_raise(TypeError){ @string1[0,1] = nil }
      assert_raise(IndexError){ @string1[0,-1] = 'x' }
      assert_raise(IndexError){ @string1[99,1] = 'x' }
   end

   def test_aset_regex_expected_errors
      assert_raise(TypeError){ @string1[/qw/] = nil }
      assert_raises(IndexError){ @string1[/foobar/] = 'foo' }
   end

   def test_aset_string_expected_errors
      assert_raise(TypeError){ @string1['qw'] = nil }
      assert_raise(IndexError){ @string1['x'] = 'y' }
   end

   def teardown
      @string1 = nil
      @string2 = nil
   end
end
