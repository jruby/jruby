######################################################################
# tc_gid.rb
#
# Test case for the FileStat#gid instance method.
######################################################################
require 'test/unit'

class TC_FileStat_Gid_Instance < Test::Unit::TestCase
   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_gid_basic
      assert_respond_to(@stat, :gid)
      assert_kind_of(Fixnum, @stat.gid)
   end

   def test_gid_expected_errors
      assert_raises(ArgumentError){ @stat.gid(1) }
      assert_raises(NoMethodError){ @stat.gid = 1 }
   end

   def teardown
      @stat = nil
   end
end
