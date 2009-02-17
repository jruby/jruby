#####################################################################
# tc_atime.rb
#
# Test case for the File#atime instance method.
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Atime_InstanceMethod < Test::Unit::TestCase
   def setup
      @name = File.expand_path(__FILE__)
      @file = File.open(@name)
   end

   def test_atime
      assert_respond_to(@file, :atime)
      assert_nothing_raised{ @file.atime }
      assert_kind_of(Time, @file.atime)
   end

   def test_atime_fails_on_closed_handle
      assert_nothing_raised{ @file.close }
#      assert_raise(IOError){ @file.atime }
   end

   def test_atime_expected_errors
      assert_raises(ArgumentError){ @file.atime(@name) }
   end

   def teardown
      @file.close unless @file.closed?
      @name = nil
      @file = nil
   end
end
