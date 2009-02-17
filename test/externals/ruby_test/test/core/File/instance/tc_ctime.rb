#####################################################################
# tc_ctime.rb
#
# Test case for the File#ctime instance method.
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Ctime_InstanceMethod < Test::Unit::TestCase
   def setup
      @name = File.expand_path(__FILE__)
      @file = File.open(@name)
   end

   def test_ctime
      assert_respond_to(@file, :ctime)
      assert_nothing_raised{ @file.ctime }
      assert_kind_of(Time, @file.ctime)
   end

   def test_ctime_fails_on_closed_handle
      assert_nothing_raised{ @file.close }
#      assert_raise(IOError){ @file.ctime }
   end

   def test_ctime_expected_errors
      assert_raises(ArgumentError){ @file.ctime(@name) }
   end

   def teardown
      @file.close unless @file.closed?
      @name = nil
      @file = nil
   end
end
