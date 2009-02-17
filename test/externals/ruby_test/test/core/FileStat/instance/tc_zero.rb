#########################################################################
# tc_zero.rb
#
# Test case for the FileStat#zero? instance method.
#########################################################################
require 'test/unit'
require 'test/helper'

class TC_FileStat_Zero_Instance < Test::Unit::TestCase
   include Test::Helper

   def setup
      @stat = File::Stat.new(__FILE__)
      @file = null_device
   end

   def test_zero_basic
      assert_respond_to(@stat, :zero?)
   end

   def test_zero
      assert_equal(false, @stat.zero?)
      assert_equal(true, File::Stat.new(@file).zero?)
   end

   def test_zero_expected_errors
      assert_raises(ArgumentError){ @stat.zero?(1) }
   end

   def teardown
      @stat = nil
      @file = nil
   end
end
