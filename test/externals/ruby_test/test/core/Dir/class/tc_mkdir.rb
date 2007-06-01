######################################################################
# tc_mkdir.rb
#
# Test case for the Dir.mkdir class method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_Dir_Mkdir_Class < Test::Unit::TestCase
   include Test::Helper

   def setup
      @dir1 = "bogus1"
      @dir2 = "bogus2"
   end

   def test_mkdir_basic
      assert_respond_to(Dir, :mkdir)
      assert_nothing_raised{ Dir.mkdir(@dir1) }
      assert_nothing_raised{ Dir.mkdir(@dir2, 0777) }
   end

   def test_mkdir
      assert_equal(0, Dir.mkdir(@dir1))
      assert_equal(0, Dir.mkdir(@dir2, 0777))
      unless WINDOWS || JRUBY
         assert_equal(0, File.umask & File.stat(@dir1).mode)
      end
   end

   def test_mkdir_expected_errors
      assert_raise(TypeError){ Dir.mkdir(1) }
      assert_nothing_raised{ Dir.mkdir(@dir1) }
      assert_raise_kind_of(SystemCallError){ Dir.mkdir(@dir1) }
   end

   def teardown
      remove_dir(@dir1)
      remove_dir(@dir2)

      @dir1 = nil
      @dir2 = nil
   end
end
