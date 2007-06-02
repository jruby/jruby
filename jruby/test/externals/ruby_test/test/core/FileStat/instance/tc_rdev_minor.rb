######################################################################
# tc_rdev_minor.rb
#
# Test case for the FileStat#rdev_minor instance method.
######################################################################
require 'test/unit'

class TC_FileStat_RdevMinor_Instance < Test::Unit::TestCase
   WINDOWS = RUBY_PLATFORM.match('mswin')
   
   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_rdev_minor_basic
      assert_respond_to(@stat, :rdev_minor)
      
      if WINDOWS
         assert_nil(@stat.rdev_minor)
      else
         assert_kind_of(Fixnum, @stat.rdev_minor)
      end
   end

   def test_rdev_minor
      unless RUBY_PLATFORM.match('mswin')
         assert_equal(true, @stat.rdev_minor == 0)
         assert_equal(true, File::Stat.new('/dev/stdin').rdev_minor == 0)
      end
   end

   def test_rdev_minor_expected_errors
      assert_raises(ArgumentError){ @stat.rdev_minor(1) }
   end

   def teardown
      @stat = nil
   end
end
