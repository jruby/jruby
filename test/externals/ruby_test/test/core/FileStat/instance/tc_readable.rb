######################################################################
# tc_readable.rb
#
# Test case for the FileStat#readable? instance method.
######################################################################
require 'test/unit'

class TC_FileStat_Readable_Instance < Test::Unit::TestCase
   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_readable_basic
      assert_respond_to(@stat, :readable?)
   end

   def test_readable
      assert_equal(true, @stat.readable?)
   end

   def test_readable_expected_errors
      assert_raises(ArgumentError){ @stat.readable?(1) }
   end

   def teardown
      @stat = nil
   end
end
