######################################################################
# tc_writable.rb
#
# Test case for the FileStat#writable? instance method.
######################################################################
require 'test/unit'

class TC_FileStat_Writeable_Instance < Test::Unit::TestCase
   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_writable_basic
      assert_respond_to(@stat, :writable?)
   end

   def test_writable
      assert_equal(true, @stat.writable?)
   end

   def test_writable_expected_errors
      assert_raises(ArgumentError){ @stat.writable?(1) }
   end

   def teardown
      @stat = nil
   end
end
