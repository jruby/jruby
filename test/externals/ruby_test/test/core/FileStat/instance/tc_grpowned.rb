######################################################################
# tc_grpowned.rb
#
# Test case for the FileStat#grpowned? instance method. Most of
# these tests are skipped on MS Windows.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_FileStat_GrpOwned_InstanceMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @stat = File::Stat.new(__FILE__)

      if WINDOWS
         @user = nil
         @bool = nil
      else
         @user = Etc.getpwnam(Etc.getlogin)
         @bool = Etc.getgrgid(@user.gid).name == @user.name
         @bool = true if ROOT
      end
   end

   def test_grpowned_basic
      assert_respond_to(@stat, :grpowned?)
   end

   def test_grpowned
      if WINDOWS
         assert_equal(false, @stat.grpowned?)
      else
         assert_equal(true, @stat.grpowned?)
         assert_equal(@bool, File::Stat.new('/').grpowned?)
      end
   end

   def test_grpowned_expected_errors
      assert_raises(ArgumentError){ @stat.grpowned?(1) }
   end

   def teardown
      @stat = nil
      @bool = nil
      @user = nil
   end
end
