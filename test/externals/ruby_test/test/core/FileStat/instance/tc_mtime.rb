######################################################################
# tc_mtime.rb
#
# Test case for the FileStat#mtime instance method.
######################################################################
require 'test/unit'

class TC_FileStat_Mtime_Instance < Test::Unit::TestCase
   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_mtime_basic
      assert_respond_to(@stat, :mtime)
      assert_kind_of(Time, @stat.mtime)
   end

   def test_mtime_expected_errors
      assert_raises(ArgumentError){ @stat.mtime(1) }
      assert_raises(NoMethodError){ @stat.mtime = 1 }
   end

   def teardown
      @stat = nil
   end
end
