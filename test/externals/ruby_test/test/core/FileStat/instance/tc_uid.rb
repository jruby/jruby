######################################################################
# tc_uid.rb
#
# Test case for the FileStat#uid instance method.
######################################################################
require 'test/unit'

class TC_FileStat_Uid_Instance < Test::Unit::TestCase
   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_uid_basic
      assert_respond_to(@stat, :uid)
      assert_kind_of(Fixnum, @stat.uid)
   end

   def test_uid_expected_errors
      assert_raises(ArgumentError){ @stat.uid(1) }
      assert_raises(NoMethodError){ @stat.uid = 1 }
   end

   def teardown
      @stat = nil
   end
end
