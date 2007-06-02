#####################################################################
# tc_tr_s.rb
#
# Test case for the String#tr_s and String#tr_s! instance methods.
#####################################################################
require 'test/unit'

class TC_String_Tr_S_Instance < Test::Unit::TestCase
   def setup
      @string1 = "hello"                    # Simple
      @string2 = "C:\\Program Files\\Test"  # Backslashes
      @string3 = "\221\222\223\224\225"     # Accented characters
   end

   def test_tr_s_basic
      assert_respond_to(@string1, :tr_s)
      assert_nothing_raised{ @string1.tr_s('h', '*') }
      assert_kind_of(String, @string1.tr_s('h', '*'))
   end

   def test_tr_s_bang_basic
      assert_respond_to(@string1, :tr_s!)
      assert_nothing_raised{ @string1.tr_s!('h', '*') }
      assert_equal(nil, @string1.tr_s!('h', '*'))
   end

   def test_tr_s_single_character
      assert_equal('h*ll*', @string1.tr_s('aeiou', '*'))
      assert_equal('C:/Program Files/Test', @string2.tr_s("\\", '/'))
      assert_equal("\220\222\223\224\225", @string3.tr_s("\221", "\220"))
   end

   def test_tr_s_duplicate_character_removal
      assert_equal('hero', @string1.tr_s('l', 'r'))
      assert_equal('h*o', @string1.tr_s('el', '*'))
      assert_equal('hhxo', @string1.tr_s('el', 'hx'))
   end

   def test_tr_s_multiple_characters
      assert_equal('hipo', @string1.tr_s('el', 'ip'))
      assert_equal("C:\\Program Filsh\\Tsht", @string2.tr_s('es', 'sh'))
      assert_equal("\226\227\223\224\225", @string3.tr_s("\221\222", "\226\227"))
   end

   def test_tr_s_negation
      assert_equal('*e*o', @string1.tr_s('^aeiou', '*'))
   end

   def test_tr_s_with_range
      assert_equal('ifmp', @string1.tr_s('a-y', 'b-z'))
   end
   
   def test_tr_s_edge_cases
      assert_equal('helli', @string1.tr_s('o', 'icopter')) # To longer than From
      assert_equal('hexo', @string1.tr_s('ll', 'x'))       # From longer than To
      assert_equal('hello', @string1.tr_s('x', 'y'))       # From not found
   end
   
   def test_tr_s_expected_failures
      assert_raises(TypeError){ @string1.tr_s('l', nil) }
      assert_raises(ArgumentError){ @string1.tr_s('l') }
   end

   def teardown
      @string1 = nil
      @string2 = nil
      @string3 = nil
   end
end
