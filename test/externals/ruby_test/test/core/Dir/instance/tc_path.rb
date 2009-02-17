#########################################################
# tc_path.rb
#
# Test suite for the Dir#path instance method.
#########################################################
require 'test/unit'
require 'test/helper'

class TC_Dir_Path_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @dir_dot = Dir.new('.')
      @dir_pwd = Dir.new(Dir.pwd)
   end

   def test_path_basic
      assert_respond_to(@dir_dot, :path)
      assert_nothing_raised{ @dir_dot.path }
      assert_kind_of(String, @dir_dot.path)
   end
   
   def test_path
      assert_equal('.', @dir_dot.path)
      assert_equal(Dir.pwd, @dir_pwd.path)
   end
   
   def test_expected_errors
      assert_raises(ArgumentError){ @dir_dot.path('foo') }
   end
   
   if WINDOWS
      def test_windows_paths
         std_dir = "C:\\"
         dir = Dir.new(std_dir)
         assert_equal("C:\\", dir.path)
      end
   end
   
   def teardown
      @dir_dot.close
      @dir_pwd.close
      @dir_dot = nil
      @dir_pwd = nil
   end
end
