###############################################################################
# tc_split.rb
#
# Test suite for the File.split class method.
#
# Note: this author considers the behavior of File.split on root paths to
# be nonsensical. The POSIX spec does NOT back up the current implementation.
###############################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Split < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @path = "/foo/bar/baz.rb"
   end

   # Busted in MRI
   if WINDOWS
      def test_split_windows_unc_path
         assert_equal(['//foo/bar', 'baz'], File.split("//foo/bar/baz"))
         assert_equal(['//foo/bar', 'baz'], File.split("//foo/bar/baz/"))
         assert_equal(['//foo/bar', ''], File.split("//foo/bar"))
         assert_equal(['//foo/bar', ''], File.split("//foo/bar/"))
         assert_equal(['//foo', ''], File.split("//foo"))
         assert_equal(['//foo', ''], File.split("//foo/"))
      end

      def test_split_windows_path
         assert_equal(['C:/foo/bar', 'baz'], File.split("C:/foo/bar/baz"))
         assert_equal(["C:\\foo\\bar", "baz"], File.split("C:\\foo\\bar\\baz"))
      end

      def test_split_edge_cases_windows
         assert_equal(["C:\\", ''], File.split("C:\\"))
         assert_equal(['//foo', ''], File.split('//////foo'))
         assert_equal(['//foo', ''], File.split('//////foo///'))
         assert_equal(['//foo', ''], File.split('//////foo//bar'))
      end
   end

   def test_split_basic
      assert_respond_to(File, :split)
      assert_nothing_raised{ File.split(@path) }
      assert_kind_of(Array, File.split(@path))
   end

   def test_split
      assert_equal(['.', 'foo'], File.split('foo'))
      assert_equal(['foo', 'bar'], File.split('foo/bar'))
      assert_equal(['/foo', 'bar'], File.split('/foo/bar'))
      assert_equal(['/foo', 'bar'], File.split('/foo/bar/'))
   end
      
   def test_split_with_extension
      assert_equal(['/foo/bar', 'baz.rb'], File.split(@path))
      assert_equal(['.', 'baz.rb'], File.split('baz.rb'))
      assert_equal(['/', 'baz.rb'], File.split('/baz.rb'))
   end
   
   # An array that contains tainted elements is not tainted, but the elements
   # themselves are tainted.
   def test_split_tainted_elements
      assert_equal(false, File.split(@path).tainted?)
      assert_nothing_raised{ @path.taint }
      assert_equal(false, File.split(@path).tainted?)
      assert_equal(true, File.split(@path)[0].tainted?)
   end
   
   unless WINDOWS
      def test_split_edge_cases_unix
         assert_equal(['/', '/'], File.split('/')) # POSIX? Maybe.
         assert_equal(['/', 'foo'], File.split('//////foo'))
         assert_equal(['/', 'foo'], File.split('//////foo///'))
         assert_equal(['/foo', 'bar'], File.split('/foo/bar////'))
      end
   end

   def test_split_edge_cases
      assert_equal(['.', ''], File.split(''))
      assert_equal(['.', ' '], File.split(' '))
      assert_equal(['.', '.'], File.split('.'))
   end

   def test_split_expected_errors
      assert_raise(ArgumentError){ File.split }
      assert_raise(ArgumentError){ File.split('foo', 'bar') }
      assert_raise(TypeError){ File.split(1) }
      assert_raise(TypeError){ File.split(nil) }
   end   

   def teardown
      @path = nil
   end
end
