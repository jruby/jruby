######################################################################
# tc_close_write.rb
#
# Test case for the IO#close_write instance method.
#
# Note: This could probably use some more robust testing.
######################################################################
require 'test/unit'
require 'test/helper'
require 'socket'

class TC_IO_CloseWrite_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file = 'tc_close_write.txt'
      @handle = File.new(@file, 'wb+')
      @socket = TCPServer.new('localhost', 9897)
   end

   def test_close_write_basic
      assert_respond_to(@handle, :close_write)
      assert_respond_to(@socket, :close_write)
   end

   def test_close_write
      assert_nothing_raised{ @socket.close_write }
      assert_raises(IOError){ @socket.write('hello') }
   end

   def test_close_write_expected_errors
      assert_raise(ArgumentError){ @socket.close_write(1) }
      assert_raise(IOError){ @handle.close_write }
   end

   def teardown
      @handle.close if @handle && !@handle.closed?
      @socket.close if @socket && !@socket.closed?
      remove_file(@file)

      @file   = nil
      @handle = nil
   end
end
