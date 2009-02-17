######################################################################
# tc_close_read.rb
#
# Test case for the IO#close_read instance method.
#
# Note: This could probably use some more robust testing.
######################################################################
require 'test/unit'
require 'test/helper'
require 'socket'

class TC_IO_CloseRead_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file = 'tc_close_read.txt'
      @handle = File.new(@file, 'wb+')
      @socket = TCPServer.new('localhost', 9897)
   end

   def test_close_read_basic
      assert_respond_to(@handle, :close_read)
      assert_respond_to(@socket, :close_read)
   end

   def test_close_read
      assert_nothing_raised{ @socket.close_read }
      assert_raises(IOError){ @socket.read }
   end

   def test_close_read_expected_errors
      assert_raise(ArgumentError){ @socket.close_read(1) }
      assert_raise(IOError){ @handle.close_read }
   end

   def teardown
      @handle.close if @handle && !@handle.closed?
      @socket.close if @socket && !@socket.closed?
      remove_file(@file)

      @file   = nil
      @handle = nil
   end
end
