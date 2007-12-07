require 'test/unit'
require 'socket'

class SocketTest < Test::Unit::TestCase
  def test_tcp_socket_allows_nil_for_hostname
    assert_nothing_raised do
      server = TCPServer.new(nil, 7789)
      t = Thread.new do
        s = server.accept
        s.close
      end
      client = TCPSocket.new(nil, 7789)
      client.write ""
      t.join
    end
  end
end
