######################################################################
# tc_dev_major.rb
#
# Test case for the FileStat#dev_major instance method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_FileStat_DevMajor_InstanceMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_dev_major_basic
      assert_respond_to(@stat, :dev_major)
      if WINDOWS
         assert_nil(@stat.dev_major)
      else
         assert_kind_of(Fixnum, @stat.dev_major)
      end
   end

   def test_dev_major
      unless WINDOWS
         assert_equal(true, @stat.dev_major > 0)
      end
   end

   def test_dev_major_expected_errors
      assert_raises(ArgumentError){ @stat.dev_major(1) }
   end

   def teardown
      @stat = nil
   end
end
