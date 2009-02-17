###########################################
# tc_dirname.rb
#
# Test suite for the File.dirname method.
###########################################
require 'test/unit'
require 'test/helper'

class TC_File_Dirname_ClassMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @msg  = '=> Known issue on MS Windows'
      @path = WINDOWS ? "C:\\foo\\bar.txt" : "/foo/bar.txt"
   end
   
   def test_basic
      assert_respond_to(File, :dirname)
      assert_nothing_raised{ File.dirname("foo") }
      assert_kind_of(String, File.dirname("foo"))
   end

   def test_dirname_unix
      assert_equal(".", File.dirname("foo"))
      assert_equal("/", File.dirname("/foo"))
      assert_equal("/foo", File.dirname("/foo/bar"))
      assert_equal("/foo", File.dirname("/foo/bar.txt"))
      assert_equal("/foo/bar", File.dirname("/foo/bar/baz"))
      assert_equal(".", File.dirname("./foo"))
      assert_equal("./foo", File.dirname("./foo/bar"))      
   end

   def test_dirname_edge_cases
      assert_equal(".", File.dirname(""))
      assert_equal(".", File.dirname("."))
      assert_equal(".", File.dirname(".."))
      assert_equal("/", File.dirname("/"))
      assert_equal("/", File.dirname("/foo/"))
      assert_equal("/", File.dirname("//foo"), @msg)
      assert_equal("/", File.dirname("//foo//"))
   end

   def test_tainted_dirname_returns_tainted_string
      assert_equal(false, File.dirname(@path).tainted?)
      assert_nothing_raised{ @path.taint }
      assert_equal(true, File.dirname(@path).tainted?)
   end

   def test_dirname_expected_errors
      assert_raises(TypeError){ File.dirname(nil) }
      assert_raises(TypeError){ File.dirname(0) }
      assert_raises(TypeError){ File.dirname(true) }
      assert_raises(TypeError){ File.dirname(false) }
   end

   # Windows specific tests
   if WINDOWS
      def test_dirname_windows_forward_slash
         assert_equal("C:/", File.dirname("C:/"))
         assert_equal("C:/", File.dirname("C:/foo"))
         assert_equal("C:/foo", File.dirname("C:/foo/bar"))
         assert_equal("C:/foo", File.dirname("C:/foo/bar/"))
         assert_equal("C:/foo", File.dirname("C:/foo/bar//"))
      end

      def test_dirname_windows
         assert_equal("C:\\foo\\bar", File.dirname("C:\\foo\\bar\\baz.txt"))
         assert_equal("C:\\foo", File.dirname("C:\\foo\\bar"))
         assert_equal("C:\\foo", File.dirname("C:\\foo\\bar\\"))
         assert_equal("C:\\", File.dirname("C:\\foo"))
         assert_equal("C:\\", File.dirname("C:\\"))
      end

      def test_dirname_windows_unc
         assert_equal("\\\\foo\\bar", File.dirname("\\\\foo\\bar\\baz.txt"))
         assert_equal("\\\\foo\\bar", File.dirname("\\\\foo\\bar\\baz"))
         assert_equal("\\\\foo", File.dirname("\\\\foo"))
         assert_equal("\\\\foo\\bar", File.dirname("\\\\foo\\bar"))
      end
   end

   def teardown
      @msg  = nil
      @path = nil
   end
end
