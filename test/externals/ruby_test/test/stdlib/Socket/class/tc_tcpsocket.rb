######################################################################
# tc_tcpsocket.rb
#
# Test case for the TCPSocket class.
######################################################################
require 'test/unit'
require 'socket'

class TC_TCPSocket_Stdlib < Test::Unit::TestCase
   def setup
      @socket = nil
   end

   def test_constructor
#      assert_nothing_raised{ @socket = TCPSocket.new('localhost', 'ftp') }
#      assert_nothing_raised{ @socket = TCPSocket.new('localhost', 'ftp', 'localhost') }
#      assert_nothing_raised{ @socket = TCPSocket.new('localhost', 'ftp', 'localhost', 8973) }
   end

   def test_constructor_expected_errors
      assert_raises(ArgumentError){ TCPSocket.new }
      assert_raises(ArgumentError){ TCPSocket.new('localhost') }
#      assert_raises(SocketError, Errno::EADDRNOTAVAIL){ TCPSocket.new('localhost', nil) }
#      assert_raises(SocketError, Errno::EADDRNOTAVAIL){ TCPSocket.new('localhost', -1) }
   end

   def teardown
      @socket.close rescue nil
   end
end
