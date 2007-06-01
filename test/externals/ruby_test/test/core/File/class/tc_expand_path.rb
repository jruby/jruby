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
   end

   def test_expand_path_basic
      assert_respond_to(File, :expand_path)
      assert_nothing_raised{ File.expand_path(__FILE__) }
      assert_kind_of(String, File.expand_path(__FILE__))
   end

   def test_expand_path
      assert_equal(@pwd, File.expand_path('.'))
      assert_equal(File.dirname(@pwd), File.expand_path('..'))
      
      if WINDOWS
         assert_equal("C:/bin", File.expand_path("/bin")) # Since when?
      else
         assert_equal("/bin", File.expand_path("/bin"))
      end
   end

   # I am not entirely sure when Ruby started prefixing 'C:' automatically
   # on MS Windows.
   if WINDOWS
      def test_expand_path_with_dir
         assert_equal("C:/bin", File.expand_path("../../bin", "/tmp/x"))
         assert_equal("C:/bin", File.expand_path("../../bin", "/tmp"))
         assert_equal("C:/bin", File.expand_path("../../bin", "/"))
         assert_equal(File.join(@pwd, 'bin'), File.expand_path("../../bin", "tmp/x"))
      end
   else
      def test_expand_path_with_dir
         assert_equal("/bin", File.expand_path("../../bin", "/tmp/x"))
         assert_equal("/bin", File.expand_path("../../bin", "/tmp"))
         assert_equal("/bin", File.expand_path("../../bin", "/"))
         assert_equal(File.join(@pwd, 'bin'), File.expand_path("../../bin", "tmp/x"))
      end
   end

   unless WINDOWS
      def test_expand_path_with_tilde
         assert_equal(@home, File.expand_path("~#{@user}"))
      end
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
