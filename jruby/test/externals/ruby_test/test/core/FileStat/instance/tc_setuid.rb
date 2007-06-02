######################################################################
# tc_setuid.rb
#
# Test case for the FileStat#setuid? instance method.
######################################################################
require 'test/unit'

class TC_FileStat_Setuid_Instance < Test::Unit::TestCase
   WINDOWS = RUBY_PLATFORM.match('mswin')
   
   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_setuid_basic
      assert_respond_to(@stat, :setuid?)
   end

   # Windows always returns true
   def test_setuid
      assert_equal(false, @stat.setuid?)
      assert_equal(true, File::Stat.new('/usr/bin/passwd').setuid?) unless WINDOWS
   end

   def test_setuid_expected_errors
      assert_raises(ArgumentError){ @stat.setuid?(1) }
   end

   def teardown
      @stat = nil
   end
end
