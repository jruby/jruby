######################################################################
# tc_setgid.rb
#
# Test case for the FileStat#setgid? instance method. Most tests
# are skipped on MS Windows.
#
# TODO: I am not sure the approach I'm using actually works. Verify.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_FileStat_Setgid_InstanceMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @stat  = File::Stat.new(__FILE__)

      if WINDOWS
         @mstat = nil
         @bool  = nil
      else
         @mstat = File::Stat.new(`which mail`.chomp)
         @bool  = @mstat.mode.to_s(8)[2].chr == '2'
      end
   end

   def test_setgid_basic
      assert_respond_to(@stat, :setgid?)
   end

   # Windows always returns true
   def test_setgid
      assert_equal(false, @stat.setgid?)
      assert_equal(@bool, @mstat.setgid?) unless WINDOWS
   end

   def test_setgid_expected_errors
      assert_raises(ArgumentError){ @stat.setgid?(1) }
   end

   def teardown
      @stat  = nil
      @mstat = nil
      @bool  = nil
   end
end
