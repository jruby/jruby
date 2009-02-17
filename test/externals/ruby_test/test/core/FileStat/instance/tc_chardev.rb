######################################################################
# tc_chardev.rb
#
# Test case for the FileStat#chardev instance method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_FileStat_Chardev_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_chardev_basic
      assert_respond_to(@stat, :chardev?)
   end

   def test_chardev
      assert_equal(false, @stat.chardev?)

      unless WINDOWS
         assert_equal(true, File::Stat.new('/dev/null').chardev?)
      end
   end

   def test_chardev_expected_errors
      assert_raises(ArgumentError){ @stat.chardev?(1) }
   end

   def teardown
      @stat = nil
   end
end
