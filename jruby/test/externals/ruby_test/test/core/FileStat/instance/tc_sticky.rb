######################################################################
# tc_sticky.rb
#
# Test case for the FileStat#sticky instance method.
######################################################################
require 'test/unit'

class TC_FileStat_Sticky_Instance < Test::Unit::TestCase
   WINDOWS = RUBY_PLATFORM.match('mswin')
   
   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_sticky_basic
      assert_respond_to(@stat, :sticky?)
   end

   def test_sticky
      assert_equal(false, @stat.sticky?)
      assert_equal(true, File::Stat.new('/tmp').sticky?)
   end

   def test_sticky_expected_errors
      assert_raises(ArgumentError){ @stat.sticky?(1) }
   end

   def teardown
      @stat = nil
      @file = nil
   end
end
