##############################################################################
# tc_closed.rb
#
# Test case for the IO#closed? instance method.
#
# Because there are a limited number of IO file descriptors, I resort to
# using a file descriptor returned by File.new instead. This is acceptable
# to me since File#closed is just using the IO#closed method that it inherited.
##############################################################################
require 'test/unit'
require 'test/helper'
require 'socket'

class TC_IO_Closed_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file   = 'test_closed.txt'
      @handle = File.new(@file, 'w+')
      @socket = TCPServer.new('localhost', 9999)
   end

   def test_is_closed_basic
      assert_respond_to(@handle, :closed?)
      assert_nothing_raised{ @handle.closed? }
      assert_kind_of(Boolean, @handle.closed?)
   end

   def test_is_closed_non_duplexed
      assert_equal(false, @handle.closed?)
      assert_nothing_raised{ @handle.close }
      assert_equal(true, @handle.closed?)
   end

   # A duplexed I/O stream isn't closed until both ends are closed.
   #
   def test_is_closed_duplexed
      assert_equal(false, @socket.closed?)
      assert_nothing_raised{ @socket.close_read }
      assert_equal(false, @socket.closed?)
      assert_nothing_raised{ @socket.close_write }
      assert_equal(true, @socket.closed?)
   end

   def test_is_closed_expected_errors
      assert_raise(ArgumentError){ @handle.closed?(2) }
   end

   def teardown
      @handle.close if @handle rescue nil
      @socket.close if @socket rescue nil

      remove_file(@file)

      @handle = nil
      @socket = nil
      @file   = nil
   end
end
