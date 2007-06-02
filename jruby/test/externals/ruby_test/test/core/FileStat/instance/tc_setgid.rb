######################################################################
# tc_setgid.rb
#
# Test case for the FileStat#setgid? instance method.
######################################################################
require 'test/unit'

class TC_FileStat_Setgid_Instance < Test::Unit::TestCase
   WINDOWS = RUBY_PLATFORM.match('mswin')
   
   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_setgid_basic
      assert_respond_to(@stat, :setgid?)
   end

   # Windows always returns true
   def test_setgid
      assert_equal(false, @stat.setgid?)
      assert_equal(true, File::Stat.new('/usr/bin/mail').setgid?) unless WINDOWS
   end

   def test_setgid_expected_errors
      assert_raises(ArgumentError){ @stat.setgid?(1) }
   end

   def teardown
      @stat = nil
   end
end
