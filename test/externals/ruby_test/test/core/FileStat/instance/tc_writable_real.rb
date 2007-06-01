######################################################################
# tc_writable_real.rb
#
# Test case for the FileStat#writable_real? instance method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_FileStat_WritableReal_InstanceMethod < Test::Unit::TestCase
   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_writable_basic
      assert_respond_to(@stat, :writable_real?)
   end

   def test_writable
      assert_equal(true, @stat.writable_real?)
   end

   def test_writable_expected_errors
      assert_raises(ArgumentError){ @stat.writable_real?(1) }
   end

   def teardown
      @stat = nil
   end
end
