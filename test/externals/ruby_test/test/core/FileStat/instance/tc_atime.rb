######################################################################
# tc_atime.rb
#
# Test case for the FileStat#atime instance method.
######################################################################
require 'test/unit'

class TC_FileStat_Atime_Instance < Test::Unit::TestCase
   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_atime_basic
      assert_respond_to(@stat, :atime)
      assert_kind_of(Time, @stat.atime)
   end

   def test_atime_expected_errors
      assert_raises(ArgumentError){ @stat.atime(1) }
      assert_raises(NoMethodError){ @stat.atime = 1 }
   end

   def teardown
      @stat = nil
   end
end
