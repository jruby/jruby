######################################################################
# tc_rdev_major.rb
#
# Test case for the FileStat#rdev_major instance method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_FileStat_RdevMajor_InstanceMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_rdev_major_basic
      assert_respond_to(@stat, :rdev_major)
      
      if WINDOWS
         assert_nil(@stat.rdev_major)
      else
         assert_kind_of(Fixnum, @stat.rdev_major)
      end
   end

   def test_rdev_major
      unless WINDOWS
         assert_equal(true, @stat.rdev_major == 0)
#         assert_equal(true, File::Stat.new('/dev/stdin').rdev_major > 0)
      end
   end

   def test_rdev_major_expected_errors
      assert_raises(ArgumentError){ @stat.rdev_major(1) }
   end

   def teardown
      @stat = nil
   end
end
