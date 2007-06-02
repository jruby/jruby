###########################################
# tc_dirname.rb
#
# Test suite for the File.dirname method.
###########################################
require "test/unit"
require "test/helper"

class TC_File_Dirname < Test::Unit::TestCase
   include Test::Helper
   
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
   end

   def test_dirname_edge_cases
      assert_equal(".", File.dirname(""))
      assert_equal(".", File.dirname("."))
      assert_equal(".", File.dirname(".."))
      assert_equal("/", File.dirname("/"))
      assert_equal("/", File.dirname("/foo/"))
      assert_equal("/", File.dirname("//foo")) # Fails on MS Windows
      assert_equal("/", File.dirname("//foo//"))
   end

   def test_dirname_expected_errors
      assert_raises(TypeError){ File.dirname(nil) }
      assert_raises(TypeError){ File.dirname(0) }
      assert_raises(TypeError){ File.dirname(true) }
      assert_raises(TypeError){ File.dirname(false) }
   end

   # Windows specific tests
   if WINDOWS
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
         
      def test_dirname_windows_forward_slash
         assert_equal("C:/", File.dirname("C:/"))
         assert_equal("C:/", File.dirname("C:/foo"))
         assert_equal("C:/foo", File.dirname("C:/foo/bar"))
         assert_equal("C:/foo", File.dirname("C:/foo/bar/"))
         assert_equal("C:/foo", File.dirname("C:/foo/bar//"))
      end
   end
end
