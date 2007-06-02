######################################################################
# tc_fnmatch.rb
#
# Test case for the File.fnmatch class method and the File.fnmatch?
# alias.
######################################################################
require 'test/unit'
require 'test/helper'
require 'fileutils'

class TC_File_Fnmatch_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @pwd     = Dir.pwd
      @path1   = 'path1'
      @path2   = 'some/Relative/Path'
      @path3   = '/cats/rule/the/World'
      @winpath = 'C:\Program Files\Ruby'
      @file1   = '/home/djberge/.bashrc'
      @strict  = File::FNM_PATHNAME | File::FNM_DOTMATCH
      @dir     = 'moose/bites/are/nasti'

      Dir.chdir(base_dir(__FILE__))
      FileUtils.mkdir_p(@dir)
   end

   def test_fnmatch_basic
      assert_respond_to(File, :fnmatch)
      assert_respond_to(File, :fnmatch?)
      assert_nothing_raised{ File.fnmatch('cat', @path1) }
      assert_kind_of(Boolean, File.fnmatch('cat', @path1))
   end

   def test_fnmatch_no_metacharacters
      assert_equal(true, File.fnmatch('path1', @path1))
      assert_equal(true, File.fnmatch('some/Relative/Path', @path2))
      assert_equal(true, File.fnmatch('/cats/rule/the/World', @path3))
      assert_equal(true, File.fnmatch('/home/djberge/.bashrc', @file1))
      #assert_equal(true, File.fnmatch("C:\\Program Files\\Ruby", @winpath)) # BUG?

      assert_equal(false, File.fnmatch('path', @path1))
      assert_equal(false, File.fnmatch('some/Relative', @path2))
      assert_equal(false, File.fnmatch('cats', @path3))
      assert_equal(false, File.fnmatch('/home', @file1))
   end

   def test_fnmatch_zero_or_more_characters
      assert_equal(true, File.fnmatch('path*', @path1))
      assert_equal(true, File.fnmatch('some/Relative*', @path2))
      assert_equal(true, File.fnmatch('*cats*', @path3))
      assert_equal(true, File.fnmatch('/home*', @file1))
      assert_equal(true, File.fnmatch('*', @path1))

      assert_equal(false, File.fnmatch('p*ath', @path1))
      assert_equal(false, File.fnmatch('some/*Relative', @path2))
      assert_equal(false, File.fnmatch('*cats', @path3))
      assert_equal(false, File.fnmatch('/h*o*me', @file1))
   end

   def test_fnmatch_single_character
      assert_equal(true, File.fnmatch('path?', @path1))
      assert_equal(true, File.fnmatch('som?/Relative/Path', @path2))
      assert_equal(true, File.fnmatch('/cat?/rul?/th?/Worl?', @path3))
      assert_equal(true, File.fnmatch('/home/djberge/?bashrc', @file1))
      assert_equal(true, File.fnmatch('?????', @path1))

      assert_equal(false, File.fnmatch('path??', @path1))
      assert_equal(false, File.fnmatch('som??/Relative/Path', @path2))
      assert_equal(false, File.fnmatch('?/cat?/rul?/th?/Worl?', @path3))
      assert_equal(false, File.fnmatch('/home/djberge/?bashrc?', @file1))
      assert_equal(false, File.fnmatch('????', @path1))
   end

   def test_fnmatch_charset
      assert_equal(true, File.fnmatch('pat[a-z][1-2]', @path1))
      assert_equal(true, File.fnmatch('some[^a-z]Relative[^a-z]Path', @path2))
      assert_equal(true, File.fnmatch('/ca[s-t][s-t]/rul[a-f]/[t]he/[W-Z]orld', @path3))
      assert_equal(true, File.fnmatch('/home/djberge/[.*}{]bashrc', @file1))

      assert_equal(false, File.fnmatch('pat[a-z][3-9]', @path1))
      assert_equal(false, File.fnmatch('some[a-z]Relative[a-z]Path', @path2))
      assert_equal(false, File.fnmatch('/ca[s][s-t]/rul[a-b]/[z]he/[x-Z]orld', @path3))
      assert_equal(false, File.fnmatch('/home/djberge/[*}{]bashrc', @file1))
   end

   def test_fnmatch_escape
      assert_equal(true, File.fnmatch('path\?', 'path?'))
      assert_equal(true, File.fnmatch('som\*/Relative/Path', 'som*/Relative/Path'))
      assert_equal(true, File.fnmatch('/cat\?/rul\?/\[th\]\?/Worl\?', '/cat?/rul?/[th]?/Worl?'))
      assert_equal(true, File.fnmatch('/home/djb\*rge/\?bashrc', '/home/djb*rge/?bashrc'))
      assert_equal(true, File.fnmatch('\?\?\?\?\?', '?????'))

      assert_equal(false, File.fnmatch('path\?', @path1))
      assert_equal(false, File.fnmatch('som\*/Relative/Path', @path2))
      assert_equal(false, File.fnmatch('/cat\?/rul\?/th\?/Worl\?', @path3))
      assert_equal(false, File.fnmatch('/home/djberge/\?bashrc', @file1))
      assert_equal(false, File.fnmatch('\?\?\?\?\?', @path1))
   end

   def test_fnmatch_match_subdirectories
      assert_equal(true, File.fnmatch('**', @dir))
      assert_equal(true, File.fnmatch('moose/**', @dir))
      assert_equal(true, File.fnmatch('moose/**/nasti', @dir))
      assert_equal(true, File.fnmatch('moose/**/**/nasti', @dir))
      assert_equal(true, File.fnmatch('moose/**/**/**', @dir))

      assert_equal(false, File.fnmatch('**/moose/**', @dir))
      assert_equal(false, File.fnmatch('moose/**/bites', @dir))
      assert_equal(false, File.fnmatch('moose/**/bites/are/nasti', @dir))
      assert_equal(false, File.fnmatch('moose/**/bites/**/nasti', @dir))
      assert_equal(false, File.fnmatch('moose/bites/nasti/**', @dir))
   end

   def test_fnmatch_with_dotmatch
      assert_equal(true, File.fnmatch('*', @file1))
      assert_equal(true, File.fnmatch('*', @file1, File::FNM_DOTMATCH))
      assert_equal(true, File.fnmatch('*', '/.bashrc'))
      assert_equal(true, File.fnmatch('*', '/.bashrc', File::FNM_DOTMATCH))

      assert_equal(false, File.fnmatch('*', '.bashrc'))
   end

   def test_fnmatch_with_pathname
      assert_equal(true, File.fnmatch('*', @dir))
      assert_equal(true, File.fnmatch('moose?bites?are?nasti', @dir))

      assert_equal(false, File.fnmatch('*', @path3, File::FNM_PATHNAME))
      assert_equal(false, File.fnmatch('moose?bites?are?nasti', @dir, File::FNM_PATHNAME))
   end

   def test_fnmatch_with_noescape
      assert_equal(true, File.fnmatch('C:\Program Files\Ruby', @winpath, File::FNM_NOESCAPE))
      assert_equal(true, File.fnmatch('\[foo\]\[bar\]', '[foo][bar]'))

      assert_equal(false, File.fnmatch('C:\Program Files\Ruby', @winpath))
      assert_equal(false, File.fnmatch('\[foo\]\[bar\]', '[foo][bar]', File::FNM_NOESCAPE))
   end

   def test_fnmatch_with_casefold
      assert_equal(true, File.fnmatch('PATH1', @path1, File::FNM_CASEFOLD))
      assert_equal(true, File.fnmatch('sOmE/ReLATIVe/PaTH', @path2, File::FNM_CASEFOLD))
      assert_equal(true, File.fnmatch('/cats/RULE/the/world', @path3, File::FNM_CASEFOLD))

      assert_equal(false, File.fnmatch('PATH!', @path1, File::FNM_CASEFOLD))
      assert_equal(false, File.fnmatch('sOmE/ReLAT!Ve/PaTH', @path2, File::FNM_CASEFOLD))
      assert_equal(false, File.fnmatch('/c!ts/RULE/the/world', @path3, File::FNM_CASEFOLD))
      assert_equal(false, File.fnmatch("c:\\Jrogram files\\fuby", @winpath, File::FNM_CASEFOLD)) # BUG?
   end

   def test_fnmatch_edge_cases
      assert_equal(true, File.fnmatch("", ""))
      assert_equal(true, File.fnmatch(".", "."))
      assert_equal(true, File.fnmatch("..", ".."))

      assert_equal(false, File.fnmatch("", " "))
      assert_equal(false, File.fnmatch(".", "/."))
      assert_equal(false, File.fnmatch("..", "/.."))
   end

   def test_fnmatch_expected_errors
      assert_raises(ArgumentError){ File.fnmatch(@path1, @path1, 0, 0) }
      assert_raises(TypeError){ File.fnmatch(1, @path1) }
      assert_raises(TypeError){ File.fnmatch(@path1, 1) }
      assert_raises(TypeError){ File.fnmatch(@path1, @path2, @path3) }
   end

   def teardown
      remove_dir('moose')
      Dir.chdir(@pwd)

      @path1  = nil
      @path2  = nil
      @path3  = nil
      @file1  = nil
      @strict = nil
      @dir    = nil
   end
end
