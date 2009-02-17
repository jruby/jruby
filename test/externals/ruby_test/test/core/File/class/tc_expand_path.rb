#####################################################################
# tc_expand_path.rb
#
# Test case for the File.expand_path class method.
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_File_ExpandPath_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @user = get_user
      @home = get_home
      @pwd  = Dir.pwd
      ENV['HOME'] = ENV['USERPROFILE'] if WINDOWS
   end

   def test_expand_path_basic
      assert_respond_to(File, :expand_path)
      assert_nothing_raised{ File.expand_path(__FILE__) }
      assert_kind_of(String, File.expand_path(__FILE__))
   end

   # On MS Windows it appears that the home drive is automatically prepended
   # to the path if no path is provided.
   def test_expand_path
      assert_equal(@pwd, File.expand_path('.'))
      assert_equal(File.dirname(@pwd), File.expand_path('..'))
      assert_equal(File.dirname(File.dirname(@pwd)), File.expand_path('../..'))
   end

   if WINDOWS
      def test_expand_path_root_path_expands_to_itself
         assert_equal('C:/', File.expand_path('/'))
         assert_equal('C:/', File.expand_path('C:/'))
         assert_equal('C:/bin', File.expand_path('/bin'))
         assert_equal('C:/foo/bar', File.expand_path('C:/foo/bar'))
      end
   else
      def test_expand_path_root_path_expands_to_itself
         assert_equal('/', File.expand_path('/'))
         assert_equal("/bin", File.expand_path("/bin"))
         assert_equal('/foo/bar', File.expand_path('/foo/bar'))
      end
   end

   if WINDOWS
      def test_expand_path_unc_root_path_expands_to_itself
         assert_equal('//', File.expand_path('//'))
         assert_equal('//foo', File.expand_path('//foo'))
         assert_equal('//foo/bar', File.expand_path('//foo/bar'))
         assert_equal('//foo/bar/baz', File.expand_path('//foo/bar/baz'))
      end
   end

   if WINDOWS
      def test_expand_path_collapses_ellision
         assert_equal('C:/bar', File.expand_path('/foo/../bar'))
         assert_equal('C:/bar', File.expand_path('/../../bar'))
         assert_equal('C:/', File.expand_path('/../../bar/..'))
      end
   else
      def test_expand_path_collapses_ellision
         assert_equal('/bar', File.expand_path('/foo/../bar'))
         assert_equal('/bar', File.expand_path('/../../bar'))
         assert_equal('/', File.expand_path('/../../bar/..'))
      end
   end

   if WINDOWS
      def test_expand_path_with_dir
         assert_equal("C:/bin", File.expand_path("../../bin", "C:/tmp/x"))
         assert_equal("C:/bin", File.expand_path("../../bin", "C:/tmp"))
         assert_equal("C:/bin", File.expand_path("../../bin", "C:/"))
         assert_equal(File.join(@pwd, 'bin'), File.expand_path("../../bin", "tmp/x"))
         assert_equal(File.join(@pwd, 'tmp/x/y/bin'), File.expand_path("../bin", "tmp/x/y/z"))
      end
   else
      def test_expand_path_with_dir
         assert_equal("/bin", File.expand_path("../../bin", "/tmp/x"))
         assert_equal("/bin", File.expand_path("../../bin", "/tmp"))
         assert_equal("/bin", File.expand_path("../../bin", "/"))
         assert_equal("/bin", File.expand_path("../../../../../../../bin", "/"))
         assert_equal(File.join(@pwd, 'bin'), File.expand_path("../../bin", "tmp/x"))
         assert_equal(File.join(@pwd, 'tmp/x/y/bin'), File.expand_path("../bin", "tmp/x/y/z"))
      end
   end

   def test_expand_path_with_tilde
      assert_equal(@home, File.expand_path("~#{@user}"))
      assert_equal(File.join(@home, 'bin'), File.expand_path("~#{@user}/bin"))
   end

   # Second argument ignored if tilde is present and it's at position 0.
   def test_expand_path_with_tilde_and_dir
      assert_equal(@home, File.expand_path("~#{@user}", '.'))
      assert_equal(@home, File.expand_path("~#{@user}", '..'))
      assert_equal(@home, File.expand_path("~#{@user}", '/tmp'))
      assert_equal(@home, File.expand_path("~#{@user}", '../tmp'))
      assert_equal(File.join(@home, 'bin'), File.expand_path("~#{@user}/bin", '/tmp'))
   end

   def test_expand_path_returns_tainted_string
#      assert_equal(true, File.expand_path(__FILE__).tainted?)
   end

   def test_expand_path_expected_errors
      assert_raises(ArgumentError){ File.expand_path }
      assert_raises(TypeError){ File.expand_path(1) }
      assert_raises(TypeError){ File.expand_path(nil) }
      assert_raises(TypeError){ File.expand_path(true) }
      
      unless WINDOWS
         assert_raises(ArgumentError){ File.expand_path("~bogus") }
      end
   end

   def teardown
      @pwd  = nil
      @user = nil
      @home = nil
   end
end
