#########################################################
# tc_path.rb
#
# Test suite for the Dir#path instance method.
#########################################################
require "test/unit"

class TC_Dir_Path_Instance < Test::Unit::TestCase
   def setup
      @dir = Dir.new(".")
   end
   
   def test_basic
      assert_respond_to(@dir, :path)
      assert_nothing_raised{ @dir.path }
      assert_kind_of(String, @dir.path)
      assert_equal(".", @dir.path)
   end
   
   def test_expected_errors
      assert_raises(ArgumentError){ @dir.path("foo") }
   end
   
   if PLATFORM.match("mswin")
      def test_windows_paths
         std_dir = "C:\\"
         dir = Dir.new(std_dir)
         assert_equal("C:\\", dir.path)
      end
   end
   
   def teardown
      @dir.close
   end
end