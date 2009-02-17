######################################################################
# tc_setuid.rb
#
# Test case for the FileStat#setuid? instance method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_FileStat_Setuid_Instance < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @stat = File::Stat.new(__FILE__)
      @pass = `which passwd`.chomp unless WINDOWS
   end

   def test_setuid_basic
      assert_respond_to(@stat, :setuid?)
   end

   # Windows always returns true
   def test_setuid
      assert_equal(false, @stat.setuid?)
      assert_equal(true, File::Stat.new(@pass).setuid?) unless WINDOWS
   end

   def test_setuid_expected_errors
      assert_raises(ArgumentError){ @stat.setuid?(1) }
   end

   def teardown
      @stat = nil
      @pass = nil unless WINDOWS
   end
end
