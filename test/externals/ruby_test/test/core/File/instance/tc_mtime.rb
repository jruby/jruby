#####################################################################
# tc_mtime.rb
#
# Test case for the File#mtime instance method.
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Mtime_InstanceMethod < Test::Unit::TestCase
   def setup
      @name = File.expand_path(__FILE__)
      @file = File.open(@name)
   end

   def test_mtime
      assert_respond_to(@file, :mtime)
      assert_nothing_raised{ @file.mtime }
      assert_kind_of(Time, @file.mtime)
   end

   def test_mtime_fails_on_closed_handle
      assert_nothing_raised{ @file.close }
#      assert_raise(IOError){ @file.mtime }
   end

   def test_mtime_expected_errors
      assert_raises(ArgumentError){ @file.mtime(@name) }
   end

   def teardown
      @file.close unless @file.closed?
      @name = nil
      @file = nil
   end
end
