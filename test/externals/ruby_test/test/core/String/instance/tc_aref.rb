###########################################################
# tc_aref.rb
#
# Test suite for the String#[] instance method.
###########################################################
require "test/unit"

class StringArefTemp
   def to_int
      2
   end
end

class TC_String_Aref_InstanceMethod < Test::Unit::TestCase
   def setup
      @string1 = "qwerty"
      @string2 = "<strong>hello</strong>"
   end

   def test_aref_basic
      assert_respond_to(@string1, :[])
   end

   # str[int]
   def test_aref_index
      assert_equal(113, @string1[0])
      assert_equal(121, @string1[-1])

      assert_nil(@string1[99])
      assert_nil(@string1[-99])
   end

   # str[int, int]
   def test_aref_substring
      assert_equal("", @string1[0,0])
      assert_equal("qw", @string1[0,2])
      assert_equal("qwerty", @string1[0,100])

      assert_nil(@string1[2, -1])
   end

   # str[range]
   def test_aref_range
      assert_equal("q", @string1[0..0])
      assert_equal("qw", @string1[0..1])
      assert_equal("qwerty", @string1[0..99])

      assert_equal("qwerty", @string1[0..-1])
      assert_equal("qwert", @string1[0..-2])
      assert_equal("", @string1[0..-99])

      assert_equal("", @string1[-1..1])
      assert_equal("y", @string1[-1..-1])
   end

   # str[regexp]
   def test_aref_regexp
      assert_equal("qwerty", @string1[/.*/])
      assert_equal("q", @string1[/q/])
      assert_nil(@string1[/x/])
   end

   # str[regexp, int]
   def test_aref_regexp_matchdata
      assert_equal("<strong>hello</strong>", @string2[/<(.*?)>(.*?)<\/\1>/])
      assert_equal("strong", @string2[/<(.*?)>(.*?)<\/\1>/, 1])
      assert_equal("hello", @string2[/<(.*?)>(.*?)<\/\1>/, 2])

      assert_equal("strong", @string2[/<(.*?)>(.*?)<\/\1>/, -2]) # Heh
      assert_equal("hello", @string2[/<(.*?)>(.*?)<\/\1>/, -1])

      assert_nil(@string2[/<(.*?)>(.*?)<\/\1>/, 3])
   end

   # str[str]
   def test_aref_string
      assert_equal("qwerty", @string1["qwerty"])
      assert_equal("ert", @string1["ert"])

      assert_nil(@string1["erf"])
   end

   def test_aref_edge_cases
      assert_equal("", @string1[""]) # Correct?
      assert_equal("", ""[''])

      assert_equal("0", "0"['0'])
      assert_equal(48, "0"[0])
   end

   # This test was added as a result of ruby-core: 10805
   def test_aref_honors_to_int
      assert_equal(101, @string1[StringArefTemp.new])
   end
   
   # Inspired by JRUBY-1721
   def test_aref_and_tainted_strings
      assert_nothing_raised{ @string1.taint }
      assert_equal(true, @string1.tainted?)
      assert_equal(true, @string1[0,1].tainted?)
   end

   def test_aref_expected_errors
      assert_raise(TypeError){ @string1[nil] }
      assert_raise(TypeError){ @string1[1, nil] }
      assert_raise(TypeError){ @string1[[1]] }
      assert_raise(ArgumentError){ @string1[] }
      assert_raise(ArgumentError){ @string1[1,2,3] }
   end

   def teardown
      @string1 = nil
      @string2 = nil
   end
end
