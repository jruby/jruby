######################################################################
# tc_dev_minor.rb
#
# Test case for the FileStat#dev_minor instance method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_FileStat_DevMinor_InstanceMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_dev_minor_basic
      assert_respond_to(@stat, :dev_minor)
      
      if WINDOWS
         assert_nil(@stat.dev_minor)
      else
         assert_kind_of(Fixnum, @stat.dev_minor)
      end
   end

   def test_dev_minor
      unless WINDOWS
         assert_equal(true, @stat.dev_minor > 0)
      end
   end

   def test_dev_minor_expected_errors
      assert_raises(ArgumentError){ @stat.dev_minor(1) }
   end

   def teardown
      @stat = nil
   end
end
