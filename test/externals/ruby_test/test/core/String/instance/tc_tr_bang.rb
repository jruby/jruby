#####################################################################
# tc_tr_bang.rb
#
# Test case for the String#tr! instance method. The String#tr tests
# can be found in the tc_tr.rb file.
#####################################################################
require 'test/unit'

class TC_String_TrBang_Instance < Test::Unit::TestCase
   def setup
      @string1 = "hello"                    # Simple
      @string2 = "C:\\Program Files\\Test"  # Backslashes
      @string3 = "\221\222\223\224\225"     # Accented characters
   end

   def test_tr_bang_basic
      assert_respond_to(@string1, :tr)
      assert_nothing_raised{ @string1.tr!('h', '*') }
      assert_kind_of(String, @string1.tr!('e', '*'))
   end

   def test_tr_bang_single_character
      assert_equal('h*ll*', @string1.tr!('aeiou', '*'))
      assert_equal('C:/Program Files/Test', @string2.tr!("\\", '/'))
      assert_equal("\220\222\223\224\225", @string3.tr!("\221", "\220"))
      assert_equal(nil, 'hello'.tr!('x', 'y'))
   end

   def test_tr_bang_original_string_modified
      assert_nothing_raised{ @string1.tr!('aeiou', '*') }
      assert_equal('h*ll*', @string1) 
   end

   def test_tr_bang_multiple_characters
      assert_equal('hippo', @string1.tr!('el', 'ip'))
      assert_equal("C:\\Program Filsh\\Tsht", @string2.tr!('es', 'sh'))
      assert_equal("\226\227\223\224\225", @string3.tr!("\221\222", "\226\227"))
      assert_equal("hxexlxlxo", 'h e l l o'.tr!(' ', 'x'))
   end

   def test_tr_bang_negation
      assert_equal('*e**o', @string1.tr!('^aeiou', '*'))
      assert_equal('x x x x x', 'h e l l o'.tr!('^" "', 'x')) 
   end

   def test_tr_bang_negation_with_range
      assert_equal('he***', @string1.tr!('^a-h', '*'))
      assert_equal('hezzz', @string1.tr!('^a-h', 'i-z'))
      assert_equal('zzzzz', @string1.tr!('^h-a', 'i-z'))
   end

   def test_tr_bang_with_range
      assert_equal('ifmmp', @string1.tr!('a-y', 'b-z'))
      assert_equal('zzzzz', @string1.tr!('a-y', 'z-b'))
      assert_equal(nil, @string1.tr!('y-a', 'b-z'))
      assert_equal(nil, @string1.tr!('y-a', 'z-b'))
   end
   
   def test_tr_bang_edge_cases
      assert_equal('helli', 'hello'.tr!('o', 'icopter')) # To longer than From
      assert_equal('hexxo', 'hello'.tr!('ll', 'x'))      # From longer than To
      assert_equal(nil, 'hello'.tr!('x', 'y'))       # From not found
      assert_equal(nil, ''.tr!('x', 'y'))                  # Empty string
      assert_equal(nil, ''.tr!('', 'x'))                   # Empty string
      assert_equal(nil, ' '.tr!('x', 'y'))                # Blank string
      assert_equal("\001\001", "\000\000".tr!("\000", "\001")) # C null
   end
   
   def test_tr_bang_expected_failures
      assert_raises(TypeError){ @string1.tr!('l', nil) }
      assert_raises(TypeError){ @string1.tr!(nil, 'l') }
      assert_raises(ArgumentError){ @string1.tr!('l') }
   end

   def teardown
      @string1 = nil
      @string2 = nil
      @string3 = nil
   end
end
