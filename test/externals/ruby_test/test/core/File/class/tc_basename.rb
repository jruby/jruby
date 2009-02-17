##############################################################################
# tc_basename.rb
#
# Test suite for the File.basename method. Note that there are some known
# issues with File.basename on MS Windows. See ruby-core: 10321.
##############################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Basename_Class < Test::Unit::TestCase
   include Test::Helper

   def setup
      @msg  = '=> Incorrect implementation for MS Windows'
      @path = WINDOWS ? "C:\\foo\\bar.txt" : "/foo/bar.txt"
   end

   def test_basename_basic
      assert_respond_to(File, :basename)
      assert_nothing_raised{ File.basename("foo") }
      assert_kind_of(String, File.basename("foo"))
   end

   def test_basename_unix
      assert_equal("bar", File.basename("/foo/bar"))
      assert_equal("bar.txt", File.basename("/foo/bar.txt"))
      assert_equal("bar.c", File.basename("bar.c"))
      assert_equal("bar", File.basename("/bar"))
      assert_equal("bar", File.basename("/bar/"))
      
      # Considered UNC paths on Windows
      unless WINDOWS
         assert_equal("foo", File.basename("//foo"))
         assert_equal("baz", File.basename("//foo/bar/baz"))
      end
   end

   def test_basename_edge_cases
      assert_equal("", File.basename(""))
      assert_equal(".", File.basename("."))
      assert_equal("..", File.basename(".."))
      
      # Considered UNC paths on Windows
      unless WINDOWS
         assert_equal("foo", File.basename("//foo/"))
         assert_equal("foo", File.basename("//foo//"))
      end
   end
   
   def test_basename_with_suffix_edge_cases
      assert_equal("foo.txt", File.basename("/foo.txt", ""))
      assert_equal("", File.basename("", ""))
      assert_equal("foo.txt   ", File.basename("foo.txt   ", ""))
   end
      
   def test_basename_unix_suffix
      assert_equal("bar", File.basename("bar.c", ".c"))
      assert_equal("bar", File.basename("bar.txt", ".txt"))
      assert_equal("bar", File.basename("/bar.txt", ".txt"))
      assert_equal("bar", File.basename("/foo/bar.txt", ".txt"))
      assert_equal("bar.txt", File.basename("bar.txt", ".exe"))
      assert_equal("bar.txt", File.basename("bar.txt.exe", ".exe"))
      assert_equal("bar.txt.exe", File.basename("bar.txt.exe", ".txt"))
      assert_equal("bar", File.basename("bar.txt", ".*"))
      assert_equal("bar.txt", File.basename("bar.txt.exe", ".*"))
   end
   
   def test_basename_multiple_suffixes
      assert_equal("bar.jpg", File.basename("/foo/bar.jpg.txt", ".txt"))
      assert_equal("bar.jpg.txt", File.basename("/foo/bar.jpg.txt", ".jpg"))
      assert_equal("bar.jpg.txt", File.basename("/foo/bar.jpg.txt", ".xyz"))
   end
   
   def test_tainted_basename_returns_tainted_string
      assert_equal(false, File.basename(@path).tainted?)
      assert_nothing_raised{ @path.taint }
      assert_equal(true, File.basename(@path).tainted?)
   end

   def test_basename_expected_errors
      assert_raises(TypeError){ File.basename(nil) }
      assert_raises(TypeError){ File.basename(1) }
      assert_raises(TypeError){ File.basename("bar.txt", 1) }
      assert_raises(TypeError){ File.basename(true) }
      assert_raises(ArgumentError){ File.basename('bar.txt', '.txt', '.txt') }
   end

   # Unix systems should still be able to parse these.
   def test_basename_windows_forward_slash
      assert_equal("foo", File.basename("C:/foo"))
      assert_equal("bar", File.basename("C:/foo/bar"))
      assert_equal("bar", File.basename("C:/foo/bar/"))
      assert_equal("bar", File.basename("C:/foo/bar//"))
#      assert_equal("C:/", File.basename("C:/"), @msg)
   end

   # Tests specific to MS Windows
   if WINDOWS
      def test_basename_windows
         assert_equal("baz.txt", File.basename("C:\\foo\\bar\\baz.txt"))
         assert_equal("bar", File.basename("C:\\foo\\bar"))
         assert_equal("bar", File.basename("C:\\foo\\bar\\"))
         assert_equal("foo", File.basename("C:\\foo"))
         assert_equal("C:\\", File.basename("C:\\"), @msg)
      end

      def test_basename_windows_unc
         assert_equal("baz.txt", File.basename("\\\\foo\\bar\\baz.txt"))
         assert_equal("baz", File.basename("\\\\foo\\bar\\baz"))
         assert_equal("\\\\foo", File.basename("\\\\foo"), @msg)
         assert_equal("\\\\foo\\bar", File.basename("\\\\foo\\bar"))
      end
         
      def test_basename_windows_suffix
         assert_equal("bar", File.basename("c:\\bar.txt", ".txt"))
         assert_equal("bar", File.basename("c:\\foo\\bar.txt", ".txt"))
         assert_equal("bar.txt", File.basename("c:\\bar.txt", ".exe"))
         assert_equal("bar.txt", File.basename("c:\\bar.txt.exe", ".exe"))
         assert_equal("bar.txt.exe", File.basename("c:\\bar.txt.exe", ".txt"))
         assert_equal("bar", File.basename("c:\\bar.txt", ".*"))
         assert_equal("bar.txt", File.basename("c:\\bar.txt.exe", ".*"))
      end
   end          

   def teardown
      @msg  = nil
      @path = nil
   end
end
