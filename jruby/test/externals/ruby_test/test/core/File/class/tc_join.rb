##########################################################
# tc_join.rb
#
# Test suite for the File.join class method
##########################################################
require "test/unit"
require "test/helper"

class TC_File_Join_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @root = WINDOWS ? "C:\\" : "/"
      @dirs = ['usr', 'local', 'bin']
   end

   def test_join_basic
      assert_respond_to(File, :join)
      assert_nothing_raised{ File.join("usr", "local", "bin") }
      assert_kind_of(String, File.join("usr", "local", "bin"))
   end

   if WINDOWS
      def test_join_windows
         assert_equal("usr/local/bin", File.join(*@dirs))
         assert_equal("C:\\usr/local/bin", File.join(@root, *@dirs))
      end

      def test_edge_cases_windows
         assert_equal("", File.join(""))
         assert_equal("/foo", File.join("", "foo"))
         assert_equal("usr/local/bin", File.join("usr", "", "local", "", "bin"))
         assert_equal("\\\\usr/local", File.join("\\\\", "usr", "local"))
      end
   else
      def test_join_unix
         assert_equal("usr/local/bin", File.join(*@dirs))
         assert_equal("/usr/local/bin", File.join(@root, *@dirs))
      end

      def test_edge_cases_unix
         assert_equal("", File.join(""))
         assert_equal("/foo", File.join("", "foo"))
         assert_equal("usr/local/bin", File.join("usr", "", "local", "", "bin"))
      end
   end

   def test_expected_errors
      assert_raises(TypeError){ File.join(nil, nil) }
   end

   def teardown
      @root = nil
      @dirs = nil
   end
end
