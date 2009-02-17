######################################################################
# tc_binmode.rb
#
# Test case for the IO#binmode class method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_IO_Binmode_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file = 'test_binmode.txt'
      @handle = File.new(@file, 'w')
   end

   def test_binmode
      assert_respond_to(@handle, :binmode)
      assert_nothing_raised{ @handle.binmode }
   end

   def test_binmode_expected_failures
      assert_raise(ArgumentError){ @handle.binmode(false) }
   end

   def teardown
      @handle.close if @handle && !@handle.closed?
      remove_file(@file)
      @handle = nil
      @file = nil
   end
end
