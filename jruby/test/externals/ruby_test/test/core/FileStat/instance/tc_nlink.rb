######################################################################
# tc_nlink.rb
#
# Test case for the FileStat#nlink instance method.
######################################################################
require 'test/unit'

class TC_FileStat_NLink_Instance < Test::Unit::TestCase
   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_nlink_basic
      assert_respond_to(@stat, :nlink)
      assert_kind_of(Fixnum, @stat.nlink)
   end

   def test_nlink
      assert_equal(1, @stat.nlink)
   end

   def test_nlink_expected_errors
      assert_raises(ArgumentError){ @stat.nlink(1) }
   end

   def teardown
      @stat = nil
   end
end
