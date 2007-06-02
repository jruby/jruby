######################################################################
# tc_chardev.rb
#
# Test case for the FileStat#chardev instance method.
######################################################################
require 'test/unit'

class TC_FileStat_Chardev_Instance < Test::Unit::TestCase
   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_chardev_basic
      assert_respond_to(@stat, :chardev?)
   end

   def test_chardev
      assert_equal(false, @stat.chardev?)

      unless RUBY_PLATFORM.match('mswin')
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
