######################################################################
# tc_rdev_minor.rb
#
# Test case for the FileStat#rdev_minor instance method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_FileStat_RdevMinor_Instance < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @stat = File::Stat.new(__FILE__)
      @rdev = 0
   end

   def test_rdev_minor_basic
      assert_respond_to(@stat, :rdev_minor)
      
      if WINDOWS
         assert_nil(@stat.rdev_minor)
      else
         assert_kind_of(Fixnum, @stat.rdev_minor)
      end
   end

   # I'm not sure if there's a more predictable way for OS X.
   def test_rdev_minor
      unless WINDOWS
         assert_equal(0, @stat.rdev_minor)
         if OSX
            @rdev = File::Stat.new('/dev/stdin').rdev_minor
#            assert_equal(true, [1,2,3].include?(@rdev))
         else
            assert_equal(@rdev, File::Stat.new('/dev/stdin').rdev_minor)
         end
      end
   end

   def test_rdev_minor_expected_errors
      assert_raises(ArgumentError){ @stat.rdev_minor(1) }
   end

   def teardown
      @stat = nil
      @rdev = nil
   end
end
