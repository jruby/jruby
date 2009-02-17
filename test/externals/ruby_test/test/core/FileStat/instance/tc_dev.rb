######################################################################
# tc_dev.rb
#
# Test case for the FileStat#dev instance method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_FileStat_Dev_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_dev_basic
      assert_respond_to(@stat, :dev)
      assert_kind_of(Fixnum, @stat.dev)
   end

   def test_dev
      unless WINDOWS
         assert_equal(true, @stat.dev > 0)
      end
   end

   def test_dev_expected_errors
      assert_raises(ArgumentError){ @stat.dev(1) }
   end

   def teardown
      @stat = nil
   end
end
