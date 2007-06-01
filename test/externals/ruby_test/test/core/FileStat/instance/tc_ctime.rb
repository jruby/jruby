######################################################################
# tc_ctime.rb
#
# Test case for the FileStat#ctime instance method.
######################################################################
require 'test/unit'

class TC_FileStat_Ctime_Instance < Test::Unit::TestCase
   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_ctime_basic
      assert_respond_to(@stat, :ctime)
      assert_kind_of(Time, @stat.ctime)
   end

   def test_ctime_expected_errors
      assert_raises(ArgumentError){ @stat.ctime(1) }
      assert_raises(NoMethodError){ @stat.ctime = 1 }
   end

   def teardown
      @stat = nil
   end
end
