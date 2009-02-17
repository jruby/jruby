##############################################################################
# tc_slice.rb
#
# Test suite for the String#slice and String#slice! instance methods.
##############################################################################
require 'test/unit'

class TC_String_Slice_InstanceMethod < Test::Unit::TestCase
   def setup
      @string1 = 'qwerty'
      @string2 = '<strong>hello</strong>'
   end

   def test_slice_basic
      assert_respond_to(@string1, :slice)
      assert_respond_to(@string1, :slice!)
   end

   # str.slice(int)
   def test_slice_index
      assert_equal(113, @string1.slice(0))
      assert_equal(121, @string1.slice(-1))
      assert_nil(@string1.slice(99))
      assert_nil(@string1.slice(-99))
      assert_equal('qwerty', @string1) # Not modified
   end

   # str.slice!(int)
   def test_slice_bang_index
      assert_equal(113, @string1.slice!(0))
      assert_equal(121, @string1.slice!(-1))
      assert_nil(@string1.slice!(99))
      assert_nil(@string1.slice!(-99))
      assert_equal('wert', @string1) # Modified in place
   end

   # str.slice(int, int)
   def test_slice_substring
      assert_equal('', @string1.slice(0,0))
      assert_equal('qw', @string1.slice(0,2))
      assert_equal('qwerty', @string1.slice(0,100))

      assert_nil(@string1.slice(2, -1))
      assert_equal('qwerty', @string1) # Unmodified
   end

   # str.slice!(int, int)
   def test_slice_bang_substring
      assert_equal('', @string1.slice!(0,0))
      assert_equal('qw', @string1.slice!(0,2))
      assert_equal('erty', @string1.slice!(0,100))

      assert_nil(@string1.slice!(2, -1))
      assert_equal('', @string1) # Modified in place
   end

   # str.slice(range)
   def test_slice_range
      assert_equal('q', @string1.slice(0..0))
      assert_equal('qw', @string1.slice(0..1))
      assert_equal('qwerty', @string1.slice(0..99))

      assert_equal('qwerty', @string1.slice(0..-1))
      assert_equal('qwert', @string1.slice(0..-2))
      assert_equal('', @string1.slice(0..-99))

      assert_equal('', @string1.slice(-1..1))
      assert_equal('y', @string1.slice(-1..-1))
      assert_equal('', @string1.slice(-3..-9))

      assert_equal('qwerty', @string1) # Unmodified
   end

   # str.slice!(range)
   def test_slice_bang_range
      assert_equal('q', @string1.slice!(0..0))
      assert_equal('werty', @string1)

      assert_equal('we', @string1.slice!(0..1))
      assert_equal('rty', @string1)

      assert_equal('rty', @string1.slice!(0..99))
      assert_equal('', @string1)
   end

   # str.slice(regexp)
   def test_slice_regexp
      assert_equal('qwerty', @string1.slice(/.*/))
      assert_equal('q', @string1.slice(/q/))
      assert_nil(@string1.slice(/x/))
   end

   # str.slice!(regexp)
   def test_slice_bang_regexp
      assert_equal('qwerty', @string1.slice!(/.*/))
      assert_equal('', @string1)

      assert_equal('h', @string2.slice!(/h/))
      assert_nil(@string2.slice!(/x/))
   end

   # str.slice(regexp, int)
   def test_slice_regexp_matchdata
      assert_equal('<strong>hello</strong>', @string2.slice(/<(.*?)>(.*?)<\/\1>/))
      assert_equal('strong', @string2.slice(/<(.*?)>(.*?)<\/\1>/, 1))
      assert_equal('hello', @string2.slice(/<(.*?)>(.*?)<\/\1>/, 2))

      assert_equal('strong', @string2.slice(/<(.*?)>(.*?)<\/\1>/, -2)) # Heh
      assert_equal('hello', @string2.slice(/<(.*?)>(.*?)<\/\1>/, -1))

      assert_nil(@string2.slice(/<(.*?)>(.*?)<\/\1>/, 3))
   end

   # str.slice!(regexp, int)
   def test_slice_bang_regexp_matchdata
      assert_equal('<strong>hello</strong>', @string2.slice!(/<(.*?)>(.*?)<\/\1>/))
      assert_equal('', @string2)
   end

   # str.slice(str)
   def test_slice_string
      assert_equal('qwerty', @string1.slice('qwerty'))
      assert_equal('ert', @string1.slice('ert'))
      assert_nil(@string1.slice('erf'))
   end

   def test_slice_bang_string
      assert_equal('qwerty', @string1.slice!('qwerty'))
      assert_equal('', @string1)

      assert_nil(@string2.slice!('erf'))
      assert_equal('<strong>hello</strong>', @string2)
   end

   def test_slice_edge_cases
      assert_equal('', @string1.slice('')) # Correct?
      assert_equal('', ''.slice(''))

      assert_equal('0', '0'.slice('0'))
      assert_equal(48, '0'.slice(0))
   end
   
   # JRUBY-1721
   def test_slice_and_tainted_strings
      assert_nothing_raised{ @string1.taint }
      assert_equal(true, @string1.tainted?)
      assert_equal(true, @string1.slice(0,1).tainted?)
   end

   # JRUBY-1745
   def test_slice_float_indices
      assert_equal(113, @string1.slice(0.9))
      assert_equal(121, @string1.slice(-1.2))
      assert_equal('qw', @string1.slice(0.9, 2.2))
   end

   def test_slice_expected_errors
      assert_raises(TypeError){ @string1.slice(nil) }
      assert_raises(TypeError){ @string1.slice!(nil) }
   end

   def teardown
      @string1 = nil
      @string2 = nil
   end
end
